import os
import xml.etree.ElementTree as ET

files = [
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values-de/strings.xml',
    'app/src/main/res/values-es/strings.xml',
    'app/src/main/res/values-fr/strings.xml',
    'app/src/main/res/values-in/strings.xml',
    'app/src/main/res/values-it/strings.xml',
    'app/src/main/res/values-pt/strings.xml',
    'app/src/main/res/values-pt-rBR/strings.xml',
    'app/src/main/res/values-ro/strings.xml'
]

for file_path in files:
    if not os.path.exists(file_path):
        continue
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        continue

    # A crude but effective way to deduplicate identical <string name="...">...</string> tags
    # Since they are exactly the same lines, we can just split by lines and keep first occurrence of each string key
    lines = content.split('\n')
    seen_keys = set()
    new_lines = []
    for line in lines:
        if '<string name=' in line:
            # extract key
            start_idx = line.find('name="') + 6
            end_idx = line.find('"', start_idx)
            key = line[start_idx:end_idx]
            if key in seen_keys:
                continue # duplicate
            seen_keys.add(key)
        new_lines.append(line)
        
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(new_lines))

print("Duplicates removed.")
