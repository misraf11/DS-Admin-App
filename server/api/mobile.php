<?php
/**
 * DS Admin — Mobile App API (WooControl companion)
 *
 * Upload this file to: <dashboard>/api/mobile.php
 * Token based JSON API used by the DS Admin Android app. It reuses the same
 * database, helpers and WooCommerce services as the web dashboard, so every
 * number in the app matches the web panel exactly.
 *
 * Auth: POST ?action=login (email, password) returns a token. Send it on every
 * other request as header "X-DS-Token: <token>" (Authorization: Bearer also works).
 */
declare(strict_types=1);

require dirname(__DIR__) . '/app/bootstrap.php';
require_once dirname(__DIR__) . '/app/Support/actions.php';

use WooControl\Core\Database;
use WooControl\Core\Schema;
use WooControl\Repositories\StoreRepository;
use WooControl\Services\SyncService;
use WooControl\Services\WooCommerceClient;
use WooControl\Services\RealtimeService;

// The app never uses the PHP session; release the lock immediately so slow
// WooCommerce calls never block other requests.
if (session_status() === PHP_SESSION_ACTIVE) session_write_close();

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('X-Content-Type-Options: nosniff');

const DSM_API_VERSION = 1;
const DSM_TOKEN_DAYS = 90;

/* ----------------------------------------------------------------- helpers */

function dsm_out(array $data, int $status = 200): void {
    http_response_code($status);
    echo json_encode(['success' => $status < 400] + $data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION);
    exit;
}

function dsm_fail(string $message, int $status = 400, array $extra = []): void {
    dsm_out(['message' => $message] + $extra, $status);
}

function dsm_input(): array {
    static $input = null;
    if ($input !== null) return $input;
    $input = $_POST;
    $raw = file_get_contents('php://input');
    if (is_string($raw) && $raw !== '' && stripos((string)($_SERVER['CONTENT_TYPE'] ?? ''), 'json') !== false) {
        $json = json_decode($raw, true);
        if (is_array($json)) $input = array_merge($input, $json);
    }
    return $input;
}

function dsm_param(string $key, $default = null) {
    $in = dsm_input();
    if (array_key_exists($key, $in)) return $in[$key];
    if (array_key_exists($key, $_GET)) return $_GET[$key];
    return $default;
}

function dsm_require_post(): void {
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') dsm_fail('POST required.', 405);
}

function dsm_i($v): int { return (int)($v ?? 0); }
function dsm_f($v): float { return round((float)($v ?? 0), 2); }
function dsm_s($v): string { return trim((string)($v ?? '')); }
function dsm_ts($v): ?int {
    $v = trim((string)($v ?? ''));
    if ($v === '' || strpos($v, '0000-00-00') === 0) return null;
    $t = strtotime($v);
    return $t ?: null;
}

/** Cast a dashboard_stats() row (strings from MySQL) into clean numbers. */
function dsm_numbers(array $row): array {
    $out = [];
    foreach ($row as $k => $v) {
        if (is_numeric($v)) $out[$k] = (strpos((string)$v, '.') !== false) ? round((float)$v, 2) : (int)$v;
        else $out[$k] = $v;
    }
    return $out;
}

function dsm_token_table(): void {
    static $done = false;
    if ($done) return;
    Database::run('CREATE TABLE IF NOT EXISTS app_tokens (
        id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT UNSIGNED NOT NULL,
        token_hash CHAR(64) NOT NULL,
        device_name VARCHAR(190) NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_used_at DATETIME NULL,
        expires_at DATETIME NULL,
        UNIQUE KEY unique_token_hash (token_hash),
        INDEX idx_token_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
    $done = true;
}

function dsm_request_token(): string {
    $token = (string)($_SERVER['HTTP_X_DS_TOKEN'] ?? '');
    if ($token === '') {
        $auth = (string)($_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
        if (stripos($auth, 'Bearer ') === 0) $token = substr($auth, 7);
    }
    return trim($token);
}

function dsm_auth_user(): array {
    dsm_token_table();
    $token = dsm_request_token();
    if ($token === '' || strlen($token) < 40) dsm_fail('Login required.', 401, ['code' => 'auth']);
    $row = Database::one('SELECT t.id token_id, t.expires_at, u.* FROM app_tokens t INNER JOIN users u ON u.id=t.user_id WHERE t.token_hash=? LIMIT 1', [hash('sha256', $token)]);
    if (!$row || ($row['status'] ?? 'active') !== 'active') dsm_fail('Session expired. Please login again.', 401, ['code' => 'auth']);
    if (!empty($row['expires_at']) && strtotime((string)$row['expires_at']) < time()) {
        Database::run('DELETE FROM app_tokens WHERE id=?', [$row['token_id']]);
        dsm_fail('Session expired. Please login again.', 401, ['code' => 'auth']);
    }
    Database::run('UPDATE app_tokens SET last_used_at=NOW(), expires_at=DATE_ADD(NOW(), INTERVAL ' . DSM_TOKEN_DAYS . ' DAY) WHERE id=?', [$row['token_id']]);
    return $row;
}

function dsm_store(bool $required = true): ?array {
    $id = (int)dsm_param('store_id', 0);
    $store = $id ? StoreRepository::find($id) : StoreRepository::first();
    if (!$store && $required) dsm_fail('No WooCommerce store connected yet. Connect your store first.', 409, ['code' => 'no_store']);
    return $store;
}

function dsm_store_out(?array $store): ?array {
    if (!$store) return null;
    return [
        'id' => dsm_i($store['id']),
        'name' => dsm_s($store['store_name']),
        'url' => dsm_s($store['store_url']),
        'connection_status' => dsm_s($store['connection_status'] ?? ''),
        'realtime_status' => dsm_s($store['realtime_status'] ?? 'pending') ?: 'pending',
        'currency' => dsm_s($store['currency'] ?? 'BDT') ?: 'BDT',
        'last_sync_ts' => dsm_ts($store['last_sync_at'] ?? null),
        'last_order_sync_ts' => dsm_ts($store['last_order_sync_at'] ?? null),
        'last_product_sync_ts' => dsm_ts($store['last_product_sync_at'] ?? null),
        'last_webhook_ts' => dsm_ts($store['last_webhook_at'] ?? null),
    ];
}

function dsm_user_out(array $u): array {
    return ['id' => dsm_i($u['id']), 'name' => dsm_s($u['name'] ?? ''), 'email' => dsm_s($u['email'] ?? ''), 'role' => dsm_s($u['role'] ?? 'owner')];
}

function dsm_items_summary(array $items): array {
    if (!$items) return ['primary' => 'Product details unavailable', 'secondary' => '', 'count' => 0];
    $first = $items[0];
    $name = trim((string)($first['product_name'] ?? '')) ?: 'Product details unavailable';
    $qty = (int)($first['quantity'] ?? 1);
    $remaining = max(0, count($items) - 1);
    $qtyTotal = 0;
    foreach ($items as $it) $qtyTotal += max(1, (int)($it['quantity'] ?? 1));
    return [
        'primary' => $name . ($qty > 1 ? ' ×' . $qty : ''),
        'secondary' => $remaining > 0 ? '+' . $remaining . ' more ' . ($remaining === 1 ? 'item' : 'items') : '',
        'count' => $qtyTotal,
    ];
}

/** Load order_items for many orders in a single query. */
function dsm_items_for(array $orders): array {
    $map = [];
    if (!$orders) return $map;
    $ids = array_map('intval', array_column($orders, 'id'));
    $ph = implode(',', array_fill(0, count($ids), '?'));
    foreach (Database::all('SELECT order_id, product_name, quantity FROM order_items WHERE order_id IN (' . $ph . ') ORDER BY id ASC', $ids) as $it) {
        $map[(int)$it['order_id']][] = $it;
    }
    return $map;
}

function dsm_order_out(array $o, ?array $store, ?array $items = null, bool $withMessage = true): array {
    $phone = dsm_s($o['customer_phone'] ?? '');
    $msg = $withMessage ? whatsapp_order_message($o, $store) : '';
    $created = ($o["wc_created_at"] ?? null) ?: ($o["created_at"] ?? null);
    $out = [
        'id' => dsm_i($o['id']),
        'wc_order_id' => dsm_i($o['wc_order_id']),
        'number' => dsm_s($o['order_number'] ?? '') ?: (string)$o['wc_order_id'],
        'status' => strtolower(dsm_s($o['status'] ?? '')),
        'currency' => dsm_s($o['currency'] ?? 'BDT') ?: 'BDT',
        'total' => dsm_f($o['total_amount'] ?? 0),
        'discount_total' => dsm_f($o['discount_total'] ?? 0),
        'shipping_total' => dsm_f($o['shipping_total'] ?? 0),
        'payment_method' => dsm_s($o['payment_method'] ?? ''),
        'payment_title' => dsm_s($o['payment_method_title'] ?? ''),
        'customer_name' => dsm_s($o['customer_name'] ?? ''),
        'customer_phone' => $phone,
        'customer_email' => dsm_s($o['customer_email'] ?? ''),
        'billing_address' => dsm_s($o['billing_address'] ?? ''),
        'shipping_address' => dsm_s($o['shipping_address'] ?? ''),
        'customer_note' => dsm_s($o['customer_note'] ?? ''),
        'is_cod' => (int)($o['is_cod'] ?? 0) === 1,
        'whatsapp_sent_ts' => dsm_ts($o['whatsapp_sent_at'] ?? null),
        'created_ts' => dsm_ts($created),
        'updated_ts' => dsm_ts($o['updated_at'] ?? null),
        'whatsapp_message' => $msg,
        'whatsapp_url' => ($phone !== '' && $withMessage) ? whatsapp_url($phone, $msg) : '',
    ];
    if ($items !== null) {
        $sum = dsm_items_summary($items);
        $out['items_primary'] = $sum['primary'];
        $out['items_secondary'] = $sum['secondary'];
        $out['items_count'] = $sum['count'];
    }
    return $out;
}

function dsm_orders_out(array $orders, ?array $store, bool $withMessage = true): array {
    $map = dsm_items_for($orders);
    $out = [];
    foreach ($orders as $o) $out[] = dsm_order_out($o, $store, $map[(int)$o['id']] ?? [], $withMessage);
    return $out;
}

function dsm_product_out(array $p): array {
    $qty = $p['stock_quantity'] ?? null;
    return [
        'id' => dsm_i($p['id']),
        'wc_product_id' => dsm_i($p['wc_product_id']),
        'name' => dsm_s($p['name'] ?? ''),
        'sku' => dsm_s($p['sku'] ?? ''),
        'type' => dsm_s($p['type'] ?? ''),
        'status' => dsm_s($p['status'] ?? ''),
        'price' => dsm_f($p['price'] ?? 0),
        'regular_price' => dsm_f($p['regular_price'] ?? 0),
        'sale_price' => dsm_f($p['sale_price'] ?? 0),
        'stock_quantity' => ($qty === null || $qty === '') ? null : (int)$qty,
        'stock_status' => dsm_s($p['stock_status'] ?? ''),
        'stock_class' => stock_class($p['stock_status'] ?? '', $qty),
        'manage_stock' => (int)($p['manage_stock'] ?? 0) === 1,
        'image_url' => dsm_s($p['image_url'] ?? ''),
        'updated_ts' => dsm_ts($p['updated_at'] ?? null),
    ];
}

function dsm_log_out(array $l): array {
    return [
        'id' => dsm_i($l['id']),
        'type' => dsm_s($l['sync_type']),
        'status' => dsm_s($l['status']),
        'processed' => dsm_i($l['records_processed'] ?? 0),
        'created' => dsm_i($l['records_created'] ?? 0),
        'updated' => dsm_i($l['records_updated'] ?? 0),
        'error' => dsm_s($l['error_message'] ?? ''),
        'started_ts' => dsm_ts($l['started_at'] ?? null),
        'completed_ts' => dsm_ts($l['completed_at'] ?? null),
    ];
}

function dsm_badges(int $storeId): array {
    $pending = (int)(Database::one('SELECT COUNT(*) c FROM orders WHERE store_id=? AND status="pending"', [$storeId])['c'] ?? 0);
    $unread = (int)(Database::one('SELECT COUNT(*) c FROM notifications WHERE (store_id=? OR store_id IS NULL) AND status IN ("pending","unread")', [$storeId])['c'] ?? 0);
    return ['pending_orders' => $pending, 'unread_notifications' => $unread];
}

function dsm_client(array $store): WooCommerceClient {
    $cred = StoreRepository::credentials((int)$store['id']);
    if (!$cred) throw new RuntimeException('Store credentials missing. Reconnect store.');
    return new WooCommerceClient($cred['api_base_url'], $cred['consumer_key'], $cred['consumer_secret']);
}

function dsm_dashboard_base(): string {
    $base = rtrim((string)config_value('app_url', ''), '/');
    if ($base === '') {
        $https = !empty($_SERVER['HTTPS']) && strtolower((string)$_SERVER['HTTPS']) !== 'off';
        $dir = rtrim(str_replace('\\', '/', dirname(dirname($_SERVER['SCRIPT_NAME'] ?? '/api/mobile.php'))), '/');
        $base = ($https ? 'https' : 'http') . '://' . ($_SERVER['HTTP_HOST'] ?? 'localhost') . ($dir === '.' ? '' : $dir);
    }
    return $base;
}

/* ------------------------------------------------------------------ router */

$action = strtolower(trim((string)($_GET['action'] ?? dsm_param('action', ''))));

try {
    if ($action === 'ping') {
        dsm_out(['app' => 'WooControl', 'api_version' => DSM_API_VERSION, 'server_time' => time(), 'timezone' => app_timezone_name()]);
    }

    if ($action === 'login') {
        dsm_require_post();
        dsm_token_table();
        $email = trim((string)dsm_param('email', ''));
        $password = (string)dsm_param('password', '');
        if ($email === '' || $password === '') dsm_fail('ইমেইল ও পাসওয়ার্ড দিন।', 422);
        $user = Database::one('SELECT * FROM users WHERE email = ? AND status = "active" LIMIT 1', [$email]);
        if (!$user || !password_verify($password, (string)$user['password_hash'])) {
            usleep(800000); // slow down password guessing
            dsm_fail('ইমেইল বা পাসওয়ার্ড ভুল।', 401, ['code' => 'bad_login']);
        }
        $token = bin2hex(random_bytes(32));
        $device = mb_substr(trim((string)dsm_param('device', 'Android')), 0, 190);
        Database::run('INSERT INTO app_tokens (user_id, token_hash, device_name, created_at, last_used_at, expires_at) VALUES (?,?,?,NOW(),NOW(),DATE_ADD(NOW(), INTERVAL ' . DSM_TOKEN_DAYS . ' DAY))', [$user['id'], hash('sha256', $token), $device]);
        Database::run('UPDATE users SET last_login_at = NOW() WHERE id = ?', [$user['id']]);
        dsm_out(['token' => $token, 'user' => dsm_user_out($user), 'store' => dsm_store_out(StoreRepository::first())]);
    }

    $user = dsm_auth_user();

    switch ($action) {

    case 'logout':
        Database::run('DELETE FROM app_tokens WHERE token_hash=?', [hash('sha256', dsm_request_token())]);
        dsm_out(['message' => 'Logged out.']);

    case 'me':
        dsm_out(['user' => dsm_user_out($user), 'store' => dsm_store_out(dsm_store(false)), 'api_version' => DSM_API_VERSION]);

    /* ---------------------------------------------------------- live state */
    case 'live': {
        $store = dsm_store();
        $storeId = (int)$store['id'];
        $doSync = (string)dsm_param('sync', '1') !== '0';
        $liveSync = ['attempted' => false, 'ok' => true, 'changed' => 0, 'processed' => 0, 'mode' => 'webhook_or_recent_sync'];
        if ($doSync) {
            try {
                $service = new SyncService($storeId);
                $r = $service->reconcileRecentOrdersIfDue(2, 10);
                if (is_array($r)) { $liveSync['attempted'] = true; $liveSync['mode'] = (string)($r['mode'] ?? 'recent_day_reconcile'); $liveSync['processed'] += (int)($r['processed'] ?? 0); $liveSync['changed'] += (int)($r['created'] ?? 0) + (int)($r['updated'] ?? 0); }
                $r = $service->liveSyncOrdersIfDue(9);
                if (is_array($r)) { $liveSync['attempted'] = true; $liveSync['mode'] = (string)($r['mode'] ?? 'live_api_fallback'); $liveSync['processed'] += (int)($r['processed'] ?? 0); $liveSync['changed'] += (int)($r['created'] ?? 0) + (int)($r['updated'] ?? 0); }
                $r = $service->liveSyncProductsIfDue(60);
                if (is_array($r)) { $liveSync['attempted'] = true; $liveSync['processed'] += (int)($r['processed'] ?? 0); $liveSync['changed'] += (int)($r['created'] ?? 0) + (int)($r['updated'] ?? 0); }
            } catch (Throwable $e) {
                $liveSync['attempted'] = true; $liveSync['ok'] = false; $liveSync['mode'] = 'api_fallback_error'; $liveSync['error'] = substr($e->getMessage(), 0, 300);
            }
        }
        $store = StoreRepository::find($storeId) ?: $store;
        $ov = Database::one('SELECT MAX(updated_at) v, MAX(id) max_id, COUNT(*) c FROM orders WHERE store_id=?', [$storeId]);
        $pv = Database::one('SELECT MAX(updated_at) v FROM products WHERE store_id=?', [$storeId]);
        $tv = Database::one('SELECT MAX(updated_at) v FROM traffic_daily WHERE store_id=?', [$storeId]);
        $nv = Database::one('SELECT MAX(id) v FROM notifications WHERE store_id=? OR store_id IS NULL', [$storeId]);
        $stats = dsm_numbers(dashboard_stats($storeId));
        $badges = dsm_badges($storeId);
        $version = hash('sha256', implode('|', [
            (string)($ov['v'] ?? ''), (string)($ov['c'] ?? ''), (string)($pv['v'] ?? ''), (string)($tv['v'] ?? ''), (string)($nv['v'] ?? ''),
            (string)($store['last_webhook_at'] ?? ''), (string)($store['last_sync_at'] ?? ''), bd_date(), json_encode($stats), json_encode($badges),
        ]));
        $latest = Database::all('SELECT * FROM orders WHERE store_id=? ORDER BY id DESC LIMIT 5', [$storeId]);
        $lastWebhookTs = !empty($store['last_webhook_at']) ? strtotime((string)$store['last_webhook_at']) : 0;
        $webhookFresh = $lastWebhookTs > 0 && abs(time() - $lastWebhookTs) <= 180;
        $channel = $webhookFresh ? 'Webhook live' : ($liveSync['ok'] ? 'API live fallback' : 'Sync warning');
        dsm_out([
            'version' => $version,
            'server_time' => time(),
            'day_key' => bd_date(),
            'live_channel' => $channel,
            'live_sync' => $liveSync,
            'max_order_id' => (int)($ov['max_id'] ?? 0),
            'latest_orders' => dsm_orders_out($latest, $store, false),
            'badges' => $badges,
            'stats' => $stats,
            'store' => dsm_store_out($store),
        ]);
    }

    /* ----------------------------------------------------------- dashboard */
    case 'dashboard': {
        $store = dsm_store();
        $sid = (int)$store['id'];
        $recent = Database::all('SELECT * FROM orders WHERE store_id=? ORDER BY COALESCE(wc_created_at,created_at) DESC LIMIT 6', [$sid]);
        $urgent = Database::all('SELECT * FROM orders WHERE store_id=? AND status IN ("pending","on-hold") ORDER BY COALESCE(wc_created_at,created_at) ASC LIMIT 5', [$sid]);
        $low = Database::all('SELECT * FROM products WHERE store_id=? AND (stock_status="outofstock" OR (stock_quantity IS NOT NULL AND stock_quantity <= 5)) ORDER BY stock_quantity ASC LIMIT 5', [$sid]);
        $logs = Database::all('SELECT * FROM sync_logs WHERE store_id=? ORDER BY started_at DESC LIMIT 5', [$sid]);
        dsm_out([
            'store' => dsm_store_out($store),
            'stats' => dsm_numbers(dashboard_stats($sid)),
            'badges' => dsm_badges($sid),
            'recent_orders' => dsm_orders_out($recent, $store),
            'urgent_orders' => dsm_orders_out($urgent, $store),
            'low_stock' => array_map('dsm_product_out', $low),
            'sync_logs' => array_map('dsm_log_out', $logs),
            'missing_columns' => Schema::missingColumns(),
            'server_time' => time(),
        ]);
    }

    /* -------------------------------------------------------------- orders */
    case 'orders': {
        $store = dsm_store();
        $sid = (int)$store['id'];
        $status = strtolower(trim((string)dsm_param('status', '')));
        $q = trim((string)dsm_param('q', ''));
        $period = strtolower(trim((string)dsm_param('period', '')));
        if (!in_array($period, ['today', 'yesterday'], true)) $period = '';
        $perPage = max(5, min(50, (int)dsm_param('per_page', 20)));
        $page = max(1, (int)dsm_param('page', 1));

        $where = ['store_id=?']; $params = [$sid];
        if ($status !== '' && $status !== 'all') { $where[] = 'status=?'; $params[] = $status; }
        if ($period !== '') {
            [$a, $b] = bd_day_bounds($period === 'yesterday' ? -1 : 0);
            $where[] = 'COALESCE(wc_created_at,created_at) >= ? AND COALESCE(wc_created_at,created_at) < ?';
            $params[] = $a; $params[] = $b;
        }
        if ($q !== '') {
            $where[] = '(order_number LIKE ? OR customer_name LIKE ? OR customer_phone LIKE ? OR customer_email LIKE ? OR EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = orders.id AND oi.product_name LIKE ?))';
            $like = '%' . $q . '%';
            array_push($params, $like, $like, $like, $like, $like);
        }
        $whereSql = implode(' AND ', $where);
        $total = (int)(Database::one('SELECT COUNT(*) c FROM orders WHERE ' . $whereSql, $params)['c'] ?? 0);
        $pages = max(1, (int)ceil($total / $perPage));
        $offset = ($page - 1) * $perPage;
        $rows = $page > $pages ? [] : Database::all('SELECT * FROM orders WHERE ' . $whereSql . ' ORDER BY COALESCE(wc_created_at,created_at) DESC, id DESC LIMIT ' . $perPage . ' OFFSET ' . $offset, $params);

        $cw = ['store_id=?']; $cp = [$sid];
        if ($period !== '') {
            [$a, $b] = bd_day_bounds($period === 'yesterday' ? -1 : 0);
            $cw[] = 'COALESCE(wc_created_at,created_at) >= ? AND COALESCE(wc_created_at,created_at) < ?';
            $cp[] = $a; $cp[] = $b;
        }
        $counts = ['all' => 0];
        foreach (Database::all('SELECT status, COUNT(*) c FROM orders WHERE ' . implode(' AND ', $cw) . ' GROUP BY status', $cp) as $c) {
            $counts[strtolower((string)$c['status'])] = (int)$c['c'];
            $counts['all'] += (int)$c['c'];
        }
        dsm_out([
            'orders' => dsm_orders_out($rows, $store),
            'counts' => $counts,
            'page' => $page,
            'per_page' => $perPage,
            'total' => $total,
            'total_pages' => $pages,
            'has_more' => $page < $pages,
        ]);
    }

    case 'order': {
        $store = dsm_store();
        $id = (int)dsm_param('id', 0);
        $o = Database::one('SELECT * FROM orders WHERE id=? AND store_id=?', [$id, $store['id']]);
        if (!$o) dsm_fail('Order not found.', 404);
        $items = Database::all('SELECT oi.*, p.image_url FROM order_items oi LEFT JOIN products p ON p.store_id=oi.store_id AND p.wc_product_id=oi.wc_product_id WHERE oi.order_id=? ORDER BY oi.id ASC', [$o['id']]);
        $status = strtolower((string)$o['status']);
        $next = null;
        if (!in_array($status, ['processing', 'completed', 'cancelled', 'failed', 'refunded'], true)) $next = 'processing';
        elseif ($status === 'processing') $next = 'completed';
        dsm_out([
            'order' => dsm_order_out($o, $store, $items),
            'items' => array_map(function ($it) {
                return [
                    'id' => dsm_i($it['id']),
                    'product_id' => dsm_i($it['wc_product_id'] ?? 0),
                    'name' => dsm_s($it['product_name'] ?? 'Product'),
                    'sku' => dsm_s($it['sku'] ?? ''),
                    'quantity' => max(1, dsm_i($it['quantity'] ?? 1)),
                    'subtotal' => dsm_f($it['subtotal'] ?? 0),
                    'total' => dsm_f($it['total'] ?? 0),
                    'image_url' => dsm_s($it['image_url'] ?? ''),
                ];
            }, $items),
            'next_status' => $next,
            'can_cancel' => !in_array($status, ['completed', 'cancelled', 'failed', 'refunded'], true),
            'statuses' => ['pending', 'processing', 'on-hold', 'completed', 'cancelled', 'refunded', 'failed'],
        ]);
    }

    case 'order_status': {
        dsm_require_post();
        $store = dsm_store();
        $id = (int)dsm_param('order_id', 0);
        $status = strtolower(trim((string)dsm_param('status', '')));
        $allowed = ['pending', 'processing', 'on-hold', 'completed', 'cancelled', 'refunded', 'failed'];
        if (!in_array($status, $allowed, true)) dsm_fail('Invalid order status.', 422);
        $o = Database::one('SELECT * FROM orders WHERE id=? AND store_id=?', [$id, $store['id']]);
        if (!$o) dsm_fail('Order not found.', 404);
        dsm_client($store)->put('orders/' . $o['wc_order_id'], ['status' => $status]);
        Database::run('UPDATE orders SET status=?, updated_at=NOW() WHERE id=?', [$status, $id]);
        $o = Database::one('SELECT * FROM orders WHERE id=?', [$id]);
        dsm_out(['message' => 'Order #' . ($o['order_number'] ?: $o['wc_order_id']) . ' moved to ' . ucfirst($status) . '.', 'order' => dsm_order_out($o, $store, null)]);
    }

    case 'whatsapp_mark': {
        dsm_require_post();
        $store = dsm_store();
        $id = (int)dsm_param('order_id', 0);
        $mark = (int)dsm_param('mark', 1) === 1;
        $o = Database::one('SELECT id, order_number, wc_order_id FROM orders WHERE id=? AND store_id=?', [$id, $store['id']]);
        if (!$o) dsm_fail('Order not found.', 404);
        $no = $o['order_number'] ?: $o['wc_order_id'];
        if ($mark) {
            Database::run('UPDATE orders SET whatsapp_sent_at=NOW(), whatsapp_sent_note=?, updated_at=NOW() WHERE id=?', ['Marked from DS Admin app', $id]);
            $row = Database::one('SELECT whatsapp_sent_at FROM orders WHERE id=?', [$id]);
            dsm_out(['marked' => true, 'whatsapp_sent_ts' => dsm_ts($row['whatsapp_sent_at'] ?? null), 'message' => 'Order #' . $no . ' marked as WhatsApp messaged.']);
        }
        Database::run('UPDATE orders SET whatsapp_sent_at=NULL, whatsapp_sent_note=NULL, updated_at=NOW() WHERE id=?', [$id]);
        dsm_out(['marked' => false, 'whatsapp_sent_ts' => null, 'message' => 'WhatsApp mark removed for order #' . $no . '.']);
    }

    /* ------------------------------------------------------------ products */
    case 'products': {
        $store = dsm_store();
        $sid = (int)$store['id'];
        $q = trim((string)dsm_param('q', ''));
        $stock = trim((string)dsm_param('stock', ''));
        $where = ['store_id=?']; $params = [$sid];
        if ($q !== '') { $where[] = '(name LIKE ? OR sku LIKE ?)'; $like = '%' . $q . '%'; array_push($params, $like, $like); }
        if ($stock === 'low') $where[] = '(stock_status="outofstock" OR (stock_quantity IS NOT NULL AND stock_quantity <= 5))';
        if ($stock === 'out') $where[] = 'stock_status="outofstock"';
        if ($stock === 'in') $where[] = 'stock_status="instock"';
        $rows = Database::all('SELECT * FROM products WHERE ' . implode(' AND ', $where) . ' ORDER BY name ASC LIMIT 300', $params);
        $stats = dsm_numbers(dashboard_stats($sid));
        dsm_out([
            'products' => array_map('dsm_product_out', $rows),
            'stats' => [
                'total_products' => (int)($stats['total_products'] ?? 0),
                'instock' => (int)($stats['instock'] ?? 0),
                'lowstock' => (int)($stats['lowstock'] ?? 0),
                'outstock' => (int)($stats['outstock'] ?? 0),
            ],
        ]);
    }

    case 'product_stock': {
        dsm_require_post();
        $store = dsm_store();
        $id = (int)dsm_param('product_id', 0);
        $stock = trim((string)dsm_param('stock_quantity', ''));
        if ($stock === '' || !is_numeric($stock)) dsm_fail('Stock quantity valid number হতে হবে।', 422);
        $p = Database::one('SELECT * FROM products WHERE id=? AND store_id=?', [$id, $store['id']]);
        if (!$p) dsm_fail('Product not found.', 404);
        $new = (int)$stock;
        dsm_client($store)->put('products/' . $p['wc_product_id'], ['manage_stock' => true, 'stock_quantity' => $new]);
        $ss = $new > 0 ? 'instock' : 'outofstock';
        Database::run('UPDATE products SET stock_quantity=?, stock_status=?, manage_stock=1, updated_at=NOW() WHERE id=?', [$new, $ss, $id]);
        $p = Database::one('SELECT * FROM products WHERE id=?', [$id]);
        dsm_out(['message' => 'Stock updated for ' . $p['name'] . '.', 'product' => dsm_product_out($p)]);
    }

    /* ----------------------------------------------------------- customers */
    case 'customers': {
        $store = dsm_store();
        $q = trim((string)dsm_param('q', ''));
        $where = 'store_id=?'; $params = [$store['id']];
        if ($q !== '') { $where .= ' AND (customer_name LIKE ? OR customer_phone LIKE ? OR customer_email LIKE ?)'; $like = '%' . $q . '%'; array_push($params, $like, $like, $like); }
        $rows = Database::all('SELECT customer_name, customer_phone, customer_email, COUNT(*) orders_count, COALESCE(SUM(CASE WHEN status NOT IN ("cancelled","failed","refunded") THEN total_amount ELSE 0 END),0) total_spent, COALESCE(SUM(status="cancelled"),0) cancelled_count, MAX(COALESCE(wc_created_at,created_at)) last_order FROM orders WHERE ' . $where . ' GROUP BY customer_phone, customer_email, customer_name ORDER BY last_order DESC LIMIT 200', $params);
        $out = [];
        foreach ($rows as $c) {
            $cancel = (int)$c['cancelled_count'];
            $risk = $cancel >= 2 ? 'high' : ($cancel === 1 ? 'medium' : 'low');
            $msg = 'আসসালামু আলাইকুম ' . $c['customer_name'] . ', DigitalSeba থেকে বলছি। আপনার অর্ডার সংক্রান্ত সহায়তার জন্য যোগাযোগ করছি।';
            $phone = dsm_s($c['customer_phone']);
            $out[] = [
                'name' => dsm_s($c['customer_name']),
                'phone' => $phone,
                'email' => dsm_s($c['customer_email']),
                'orders_count' => (int)$c['orders_count'],
                'total_spent' => dsm_f($c['total_spent']),
                'cancelled_count' => $cancel,
                'risk' => $risk,
                'last_order_ts' => dsm_ts($c['last_order']),
                'whatsapp_url' => $phone !== '' ? whatsapp_url($phone, $msg) : '',
            ];
        }
        dsm_out(['customers' => $out]);
    }

    case 'customer_orders': {
        $store = dsm_store();
        $phone = trim((string)dsm_param('phone', ''));
        $email = trim((string)dsm_param('email', ''));
        $name = trim((string)dsm_param('name', ''));
        $rows = Database::all('SELECT * FROM orders WHERE store_id=? AND COALESCE(customer_phone,"")=? AND COALESCE(customer_email,"")=? AND COALESCE(customer_name,"")=? ORDER BY COALESCE(wc_created_at,created_at) DESC LIMIT 100', [$store['id'], $phone, $email, $name]);
        dsm_out(['orders' => dsm_orders_out($rows, $store)]);
    }

    /* ------------------------------------------------------------ whatsapp */
    case 'whatsapp': {
        $store = dsm_store();
        $pending = Database::all('SELECT * FROM orders WHERE store_id=? AND status="pending" ORDER BY COALESCE(wc_created_at,created_at) ASC LIMIT 100', [$store['id']]);
        $cod = 0; foreach ($pending as $po) if ((int)$po['is_cod'] === 1) $cod++;
        dsm_out([
            'pending_count' => count($pending),
            'cod_count' => $cod,
            'templates' => [
                ['title' => 'Pending confirmation', 'description' => 'অর্ডার confirm করার জন্য customer reply চাইবে।'],
                ['title' => 'COD verification', 'description' => 'COD order phone/address verify করতে।'],
                ['title' => 'Address confirmation', 'description' => 'Delivery address confirm করতে।'],
                ['title' => 'Cancelled follow-up', 'description' => 'Cancelled order recovery করতে।'],
            ],
            'queue' => dsm_orders_out($pending, $store),
        ]);
    }

    /* ------------------------------------------------------- notifications */
    case 'notifications': {
        $store = dsm_store();
        $rows = Database::all('SELECT * FROM notifications WHERE store_id=? OR store_id IS NULL ORDER BY created_at DESC, id DESC LIMIT 100', [$store['id']]);
        dsm_out([
            'notifications' => array_map(function ($n) {
                return [
                    'id' => dsm_i($n['id']),
                    'type' => dsm_s($n['type']),
                    'title' => dsm_s($n['title']),
                    'message' => dsm_s($n['message'] ?? ''),
                    'status' => dsm_s($n['status'] ?? ''),
                    'unread' => in_array((string)($n['status'] ?? ''), ['pending', 'unread'], true),
                    'created_ts' => dsm_ts($n['created_at'] ?? null),
                ];
            }, $rows),
            'badges' => dsm_badges((int)$store['id']),
        ]);
    }

    case 'notifications_read': {
        dsm_require_post();
        $store = dsm_store();
        $id = (int)dsm_param('id', 0);
        if ($id > 0) Database::run('UPDATE notifications SET status="read" WHERE id=? AND (store_id=? OR store_id IS NULL)', [$id, $store['id']]);
        else Database::run('UPDATE notifications SET status="read" WHERE (store_id=? OR store_id IS NULL) AND status IN ("pending","unread")', [$store['id']]);
        dsm_out(['message' => 'Notifications marked as read.', 'badges' => dsm_badges((int)$store['id'])]);
    }

    /* ------------------------------------------------------------- reports */
    case 'reports': {
        $store = dsm_store();
        $sid = (int)$store['id'];
        $byStatus = Database::all('SELECT status, COUNT(*) c, COALESCE(SUM(total_amount),0) total FROM orders WHERE store_id=? GROUP BY status ORDER BY c DESC', [$sid]);
        $payments = Database::all('SELECT COALESCE(payment_method_title,payment_method,"Unknown") method, COUNT(*) c, COALESCE(SUM(total_amount),0) total FROM orders WHERE store_id=? AND status NOT IN ("cancelled","failed","refunded") GROUP BY method ORDER BY c DESC LIMIT 8', [$sid]);
        $top = Database::all('SELECT oi.product_name, SUM(oi.quantity) units, COALESCE(SUM(oi.total),0) revenue FROM order_items oi INNER JOIN orders o ON o.id=oi.order_id WHERE oi.store_id=? AND o.status NOT IN ("cancelled","failed","refunded") GROUP BY oi.wc_product_id,oi.product_name ORDER BY revenue DESC LIMIT 10', [$sid]);
        $traffic = Database::all('SELECT stat_date,visitors,pageviews,sessions FROM traffic_daily WHERE store_id=? ORDER BY stat_date DESC LIMIT 14', [$sid]);
        $days = [];
        for ($i = 6; $i >= 0; $i--) {
            [$a, $b] = bd_day_bounds(-$i);
            $r = Database::one('SELECT COUNT(*) orders, COALESCE(SUM(CASE WHEN status NOT IN ("cancelled","failed","refunded") THEN total_amount ELSE 0 END),0) sales FROM orders WHERE store_id=? AND COALESCE(wc_created_at,created_at) >= ? AND COALESCE(wc_created_at,created_at) < ?', [$sid, $a, $b]);
            $days[] = ['date' => substr($a, 0, 10), 'orders' => (int)($r['orders'] ?? 0), 'sales' => dsm_f($r['sales'] ?? 0)];
        }
        dsm_out([
            'stats' => dsm_numbers(dashboard_stats($sid)),
            'daily' => $days,
            'by_status' => array_map(fn($r) => ['status' => strtolower((string)$r['status']), 'count' => (int)$r['c'], 'total' => dsm_f($r['total'])], $byStatus),
            'payments' => array_map(fn($r) => ['method' => dsm_s($r['method']) ?: 'Unknown', 'count' => (int)$r['c'], 'total' => dsm_f($r['total'])], $payments),
            'top_products' => array_map(fn($r) => ['name' => dsm_s($r['product_name']), 'units' => (int)$r['units'], 'revenue' => dsm_f($r['revenue'])], $top),
            'traffic' => array_map(fn($r) => ['date' => (string)$r['stat_date'], 'visitors' => (int)$r['visitors'], 'pageviews' => (int)$r['pageviews'], 'sessions' => (int)$r['sessions']], $traffic),
        ]);
    }

    case 'sync_logs': {
        $store = dsm_store();
        $logs = Database::all('SELECT * FROM sync_logs WHERE store_id=? ORDER BY started_at DESC LIMIT 200', [$store['id']]);
        dsm_out(['logs' => array_map('dsm_log_out', $logs)]);
    }

    /* -------------------------------------------------------------- health */
    case 'health': {
        $store = dsm_store();
        $sid = (int)$store['id'];
        $stats = dsm_numbers(dashboard_stats($sid));
        $last = Database::one('SELECT * FROM sync_logs WHERE store_id=? ORDER BY started_at DESC LIMIT 1', [$sid]);
        dsm_out([
            'store' => dsm_store_out($store),
            'missing_columns' => Schema::missingColumns(),
            'total_orders' => (int)($stats['total_orders'] ?? 0),
            'total_products' => (int)($stats['total_products'] ?? 0),
            'last_log' => $last ? dsm_log_out($last) : null,
        ]);
    }

    /* ------------------------------------------------------------ settings */
    case 'settings': {
        $store = dsm_store();
        $webhook = '';
        try { $webhook = (new RealtimeService((int)$store['id']))->deliveryUrl(); } catch (Throwable $ignored) {}
        dsm_out([
            'store' => dsm_store_out($store),
            'user' => dsm_user_out($user),
            'webhook_url' => $webhook,
            'cron_url' => dsm_dashboard_base() . '/cron/instant-sync.php',
            'dashboard_url' => dsm_dashboard_base(),
            'whatsapp_template' => whatsapp_order_template((int)$store['id']),
            'whatsapp_template_default' => whatsapp_order_template_default(),
            'placeholders' => [
                ['key' => '{customer_name}', 'label' => 'Customer name'],
                ['key' => '{order_number}', 'label' => 'Order number'],
                ['key' => '{total_amount}', 'label' => 'Order total'],
                ['key' => '{product_name}', 'label' => 'First product name'],
                ['key' => '{products}', 'label' => 'Full ordered product list'],
                ['key' => '{items_count}', 'label' => 'Total item quantity'],
                ['key' => '{products_summary}', 'label' => 'Compact products summary'],
                ['key' => '{store_name}', 'label' => 'Store name'],
                ['key' => '{customer_phone}', 'label' => 'Customer phone'],
                ['key' => '{status}', 'label' => 'Order status'],
            ],
        ]);
    }

    case 'save_template': {
        dsm_require_post();
        $store = dsm_store();
        $t = trim((string)dsm_param('template', ''));
        if ($t === '') $t = whatsapp_order_template_default();
        save_store_setting((int)$store['id'], 'whatsapp_order_template', $t);
        dsm_out(['message' => 'WhatsApp template saved successfully.', 'whatsapp_template' => $t]);
    }

    /* ------------------------------------------------------- sync & tools */
    case 'run': {
        dsm_require_post();
        $store = dsm_store();
        $sid = (int)$store['id'];
        $type = strtolower(trim((string)dsm_param('type', '')));
        if ($type === 'repair_database') {
            Schema::ensure();
            $missing = Schema::missingColumns();
            if ($missing) dsm_fail('Database repair attempted, but still missing: ' . implode(', ', $missing), 500);
            dsm_out(['message' => 'Database schema repaired successfully. এখন Sync Orders চাপুন।']);
        }
        if ($type === 'api_test') {
            $r = (new SyncService($sid))->test();
            dsm_out(['message' => 'API healthy: ' . ($r['name'] ?? 'WooCommerce') . ' · WooCommerce ' . ($r['wc_version'] ?? 'OK'), 'result' => $r]);
        }
        if ($type === 'sync_orders' || $type === 'sync_products') {
            $svc = new SyncService($sid);
            $r = $type === 'sync_products' ? $svc->syncProducts() : $svc->syncOrders();
            $label = $type === 'sync_products' ? 'product' : 'order';
            dsm_out(['message' => 'Fast ' . $label . ' sync complete in ' . (int)($r['elapsed_ms'] ?? 0) . 'ms · new ' . (int)($r['created'] ?? 0) . ', updated ' . (int)($r['updated'] ?? 0), 'changed' => (int)($r['created'] ?? 0) + (int)($r['updated'] ?? 0), 'result' => $r]);
        }
        if ($type === 'setup_realtime') {
            $rt = new RealtimeService($sid);
            try {
                $r = $rt->ensureWebhooks(true);
            } catch (Throwable $e) {
                $rt->markError($e->getMessage());
                throw $e;
            }
            dsm_out(['message' => 'Realtime connection active · ' . ((int)($r['created'] ?? 0) + (int)($r['updated'] ?? 0)) . ' webhook(s) configured.', 'result' => $r]);
        }
        dsm_fail('Unknown action type.', 422);
    }

    /* ------------------------------------------------------------- connect */
    case 'connect': {
        $existing = StoreRepository::first();
        if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') {
            $cred = $existing ? StoreRepository::credentials((int)$existing['id']) : null;
            $mask = function (string $v): string { return $v === '' ? '' : substr($v, 0, 6) . str_repeat('•', 8) . substr($v, -4); };
            dsm_out([
                'store' => dsm_store_out($existing),
                'consumer_key_hint' => $mask((string)($cred['consumer_key'] ?? '')),
                'consumer_secret_hint' => $mask((string)($cred['consumer_secret'] ?? '')),
            ]);
        }
        $name = trim((string)dsm_param('store_name', ''));
        $url = rtrim(trim((string)dsm_param('store_url', '')), '/');
        $key = trim((string)dsm_param('consumer_key', ''));
        $secret = trim((string)dsm_param('consumer_secret', ''));
        if ($existing && ($key === '' || $secret === '')) {
            $cred = StoreRepository::credentials((int)$existing['id']);
            if ($key === '') $key = (string)($cred['consumer_key'] ?? '');
            if ($secret === '') $secret = (string)($cred['consumer_secret'] ?? '');
        }
        if ($name === '' || $url === '' || $key === '' || $secret === '') dsm_fail('সব field পূরণ করুন।', 422);
        if (!filter_var($url, FILTER_VALIDATE_URL)) dsm_fail('Store URL valid না। Example: https://digitalseba.shop', 422);
        $host = parse_url($url, PHP_URL_HOST) ?: '';
        $appHost = parse_url((string)config_value('app_url', ''), PHP_URL_HOST) ?: ($_SERVER['HTTP_HOST'] ?? '');
        if ($host && $appHost && strtolower($host) === strtolower($appHost)) dsm_fail('Store URL হিসেবে dashboard/admin URL দেওয়া যাবে না। আসল WooCommerce site URL দিন।', 422);
        (new WooCommerceClient($url, $key, $secret))->get('system_status');
        $storeId = StoreRepository::upsert($name, $url, $key, $secret);
        dsm_out(['message' => 'Store connected and WooCommerce API verified. এখন Sync Orders করুন।', 'store' => dsm_store_out(StoreRepository::find($storeId))]);
    }

    /* -------------------------------------------------------------- search */
    case 'search': {
        $store = dsm_store();
        $sid = (int)$store['id'];
        $q = trim((string)dsm_param('q', ''));
        if (mb_strlen($q) < 1) dsm_out(['orders' => [], 'products' => [], 'customers' => []]);
        $like = '%' . $q . '%';
        $orders = Database::all('SELECT * FROM orders WHERE store_id=? AND (order_number LIKE ? OR customer_name LIKE ? OR customer_phone LIKE ? OR customer_email LIKE ? OR EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = orders.id AND oi.product_name LIKE ?)) ORDER BY COALESCE(wc_created_at,created_at) DESC LIMIT 15', [$sid, $like, $like, $like, $like, $like]);
        $products = Database::all('SELECT * FROM products WHERE store_id=? AND (name LIKE ? OR sku LIKE ?) ORDER BY name ASC LIMIT 15', [$sid, $like, $like]);
        $customers = Database::all('SELECT customer_name, customer_phone, customer_email, COUNT(*) orders_count, MAX(COALESCE(wc_created_at,created_at)) last_order FROM orders WHERE store_id=? AND (customer_name LIKE ? OR customer_phone LIKE ? OR customer_email LIKE ?) GROUP BY customer_phone, customer_email, customer_name ORDER BY last_order DESC LIMIT 15', [$sid, $like, $like, $like]);
        dsm_out([
            'orders' => dsm_orders_out($orders, $store),
            'products' => array_map('dsm_product_out', $products),
            'customers' => array_map(fn($c) => ['name' => dsm_s($c['customer_name']), 'phone' => dsm_s($c['customer_phone']), 'email' => dsm_s($c['customer_email']), 'orders_count' => (int)$c['orders_count'], 'last_order_ts' => dsm_ts($c['last_order'])], $customers),
        ]);
    }

    default:
        dsm_fail('Unknown action.', 404);
    }
} catch (Throwable $e) {
    dsm_fail($e->getMessage() ?: 'Server error.', 500, ['code' => 'server']);
}
