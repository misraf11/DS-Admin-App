"""Tiny uiautomator helper: tap elements by text / content-desc, type, swipe."""
import re, subprocess, sys, time, xml.etree.ElementTree as ET

def sh(*a):
    return subprocess.run(['adb', *a], capture_output=True, text=True).stdout

def nodes():
    for _ in range(3):
        sh('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
        x = sh('shell', 'cat', '/sdcard/ui.xml')
        if '<hierarchy' in x:
            return list(ET.fromstring(x[x.index('<hierarchy'):]).iter('node'))
        time.sleep(1)
    return []

def center(n):
    a = list(map(int, re.findall(r'\d+', n.get('bounds'))))
    return (a[0] + a[2]) // 2, (a[1] + a[3]) // 2

def find(text, mode='first', cls=None):
    ns = nodes()
    hits = [n for n in ns if (n.get('text') == text or n.get('content-desc') == text) and (cls is None or cls in n.get('class', ''))]
    if not hits:
        hits = [n for n in ns if (text in (n.get('text') or '') or text in (n.get('content-desc') or ''))]
    if not hits:
        return None
    return hits[-1] if mode == 'last' else hits[0]

def tap(x, y):
    sh('shell', 'input', 'tap', str(x), str(y))

if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd in ('tap', 'tap_last'):
        n = find(sys.argv[2], 'last' if cmd == 'tap_last' else 'first')
        if n is None:
            print('NOT FOUND', sys.argv[2]); sys.exit(0)
        tap(*center(n)); print('tapped', sys.argv[2])
    elif cmd == 'tap_prefix':
        hits = [n for n in nodes() if (n.get('text') or '').startswith(sys.argv[2])]
        if hits:
            tap(*center(hits[0])); print('tapped', hits[0].get('text'))
        else:
            print('NOT FOUND prefix', sys.argv[2])
    elif cmd == 'edit':
        idx = int(sys.argv[2])
        eds = [n for n in nodes() if 'EditText' in n.get('class', '')]
        if len(eds) > idx:
            tap(*center(eds[idx])); time.sleep(0.6)
            if len(sys.argv) > 3:
                sh('shell', 'input', 'text', sys.argv[3])
            print('typed into', idx)
        else:
            print('no edit', idx, len(eds))
    elif cmd == 'swipe_up':
        sh('shell', 'input', 'swipe', '540', '1900', '540', '700', '400')
    elif cmd == 'dump':
        for n in nodes():
            t = n.get('text') or n.get('content-desc')
            if t: print(repr(t), n.get('bounds'))
