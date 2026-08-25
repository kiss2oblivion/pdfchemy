import os
import re

def get_string_from_xml(res_dir, string_name):
    path = f"c:/Users/cucos/AndroidStudioProjects/Shrinkpdf/app/src/main/res/{res_dir}/strings.xml"
    if not os.path.exists(path):
        return string_name
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    match = re.search(f'<string name="{string_name}"[^>]*>(.*?)</string>', content)
    return match.group(1) if match else string_name

print("ro-RO consent:", get_string_from_xml("values-ro", "consent_dialog_agree"))
print("ro-RO compress:", get_string_from_xml("values-ro", "cat_compress"))
