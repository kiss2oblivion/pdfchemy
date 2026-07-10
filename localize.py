import os
import re

UI_DIR = r"c:\Users\cucos\AndroidStudioProjects\Shrinkpdf\app\src\main\java\com\example\shrinkpdf\ui"
MAIN_ACTIVITY = r"c:\Users\cucos\AndroidStudioProjects\Shrinkpdf\app\src\main\java\com\example\shrinkpdf\MainActivity.kt"
STRINGS_XML = r"c:\Users\cucos\AndroidStudioProjects\Shrinkpdf\app\src\main\res\values\strings.xml"

# Find all Text("...")
pattern = re.compile(r'Text\(\s*"([^"\\]*?)"\s*(,?.*?)\)')

def generate_key(s):
    # simple heuristic to generate a key
    key = re.sub(r'[^a-zA-Z0-9]+', '_', s.lower()).strip('_')
    if len(key) > 30:
        key = key[:30].rstrip('_')
    if not key:
        key = "empty_str"
    if key[0].isdigit():
        key = "str_" + key
    return key

new_strings = {}

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    def replacer(match):
        text_val = match.group(1)
        rest = match.group(2)
        
        # skip if it has interpolations
        if '$' in text_val:
            return match.group(0)
            
        # skip if it's already stringResource (though the regex only matches "..." literals)
        if text_val == "":
            return match.group(0)
            
        key = generate_key(text_val)
        
        # handle duplicates with different values
        if key in new_strings and new_strings[key] != text_val:
            key = key + "_1"
            
        new_strings[key] = text_val
        
        # if there are no other args, don't leave a trailing comma
        return f'Text(stringResource(R.string.{key}){rest})'
        
    new_content = pattern.sub(replacer, content)
    
    # Add imports if changed
    if new_content != content:
        if "import androidx.compose.ui.res.stringResource" not in new_content:
            new_content = new_content.replace("import androidx.compose.material3.Text\n", "import androidx.compose.material3.Text\nimport androidx.compose.ui.res.stringResource\nimport com.example.shrinkpdf.R\n")
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Processed {os.path.basename(filepath)}")

# Process files
for root, dirs, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))
process_file(MAIN_ACTIVITY)

# Append to strings.xml
with open(STRINGS_XML, 'r', encoding='utf-8') as f:
    xml_content = f.read()

# remove </resources>
xml_content = xml_content.replace("</resources>", "")

xml_content += "\n    <!-- Auto-Extracted Strings -->\n"
for key, val in new_strings.items():
    # check if key already exists in the file (crude but works)
    if f'name="{key}"' not in xml_content:
        # escape ampersands and quotes
        val_escaped = val.replace("&", "&amp;").replace("'", "\\'")
        xml_content += f'    <string name="{key}">{val_escaped}</string>\n'

xml_content += "</resources>\n"

with open(STRINGS_XML, 'w', encoding='utf-8') as f:
    f.write(xml_content)

print(f"Added {len(new_strings)} strings to strings.xml")
