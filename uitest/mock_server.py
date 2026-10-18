"""Mock of the DS Admin mobile API built from recorded fixtures (fake demo data)."""
import json, os, time, urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))
FIX = os.path.join(HERE, 'fixtures')
INDEX = json.load(open(os.path.join(FIX, 'index.json')))
TRIGGER = '/tmp/ds_new_order'
state = {'new_order': False}

def load(name):
    return json.load(open(os.path.join(FIX, name)))

def key(params):
    a = params.get('action', '')
    rest = sorted((k, v) for k, v in params.items() if k != 'action')
    return a + ('__' + '&'.join(f'{k}={v}' for k, v in rest) if rest else '')

class H(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def send(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def handle_req(self, form):
        u = urllib.parse.urlparse(self.path)
        params = {k: v[0] for k, v in urllib.parse.parse_qs(u.query, keep_blank_values=True).items()}
        action = params.get('action', '')
        with open('/tmp/mock_requests.log', 'a') as f:
            f.write(f"{time.strftime('%H:%M:%S')} {self.command} {action} {form}\n")
        if action == 'login':
            if form.get('password') != 'secret123':
                return self.send({'success': False, 'message': 'ইমেইল বা পাসওয়ার্ড ভুল।', 'code': 'bad_login'}, 401)
            me = load(INDEX['me'])
            return self.send({'success': True, 'token': 'f' * 64, 'user': me['user'], 'store': me['store']})
        if self.command == 'POST' and action != 'connect' or (action == 'connect' and self.command == 'POST'):
            msgs = {
                'order_status': f"Order moved to {form.get('status', '').capitalize()}.",
                'whatsapp_mark': 'Order marked as WhatsApp messaged.',
                'product_stock': 'Stock updated.',
                'run': 'Fast order sync complete in 412ms · new 0, updated 2',
                'save_template': 'WhatsApp template saved successfully.',
                'notifications_read': 'Notifications marked as read.',
                'connect': 'Store connected and WooCommerce API verified.',
                'logout': 'Logged out.',
            }
            out = {'success': True, 'message': msgs.get(action, 'OK')}
            if action == 'whatsapp_mark':
                out.update({'marked': form.get('mark') == '1', 'whatsapp_sent_ts': int(time.time()) if form.get('mark') == '1' else None})
            return self.send(out)
        k = key(params)
        name = INDEX.get(k)
        if name is None:
            cands = [v for kk, v in INDEX.items() if kk == action or kk.startswith(action + '__')]
            name = cands[0] if cands else None
        if name is None:
            return self.send({'success': False, 'message': 'Unknown action.'}, 404)
        data = load(name)
        if action == 'live':
            data['server_time'] = int(time.time())
            if os.path.exists(TRIGGER) or state['new_order']:
                state['new_order'] = True
                o = dict(data['latest_orders'][0])
                o.update({'id': 31, 'number': '2431', 'customer_name': 'Nusrat Jahan', 'total': 249.0, 'status': 'pending', 'created_ts': int(time.time()) - 5, 'is_cod': True})
                data['latest_orders'] = [o] + data['latest_orders']
                data['max_order_id'] = 31
                data['version'] = 'new-' + data['version']
        return self.send(data)

    def do_GET(self):
        self.handle_req({})

    def do_POST(self):
        n = int(self.headers.get('Content-Length') or 0)
        raw = self.rfile.read(n).decode() if n else ''
        form = {k: v[0] for k, v in urllib.parse.parse_qs(raw, keep_blank_values=True).items()}
        self.handle_req(form)

if __name__ == '__main__':
    ThreadingHTTPServer(('0.0.0.0', 8099), H).serve_forever()
