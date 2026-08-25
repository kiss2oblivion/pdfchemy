import os, subprocess, xml.etree.ElementTree as ET, re
ADB_PATH = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe')
def run_adb(args):
    return subprocess.run([ADB_PATH, '-s', 'emulator-5554'] + args, capture_output=True, text=True, encoding='utf-8', errors='replace').stdout.strip()
run_adb(['shell', 'uiautomator', 'dump', '/sdcard/window_dump.xml'])
run_adb(['pull', '/sdcard/window_dump.xml', 'temp_dump_phone.xml'])
with open('temp_dump_phone.xml', 'r', encoding='utf-8', errors='replace') as f:
    xml_data = f.read()
root = ET.fromstring(xml_data)
for node in root.iter('node'):
    text = node.attrib.get('text', '')
    if 'Compress' in text:
        bounds = node.attrib.get('bounds')
        coords = re.findall(r'\d+', bounds)
        x, y = (int(coords[0]) + int(coords[2])) // 2, (int(coords[1]) + int(coords[3])) // 2
        run_adb(['shell', 'input', 'tap', str(x), str(y)])
        break
