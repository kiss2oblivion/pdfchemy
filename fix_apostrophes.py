import os
import glob

files = glob.glob('app/src/main/res/values*/strings.xml')
for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace the double escaped apostrophes with a single escaped apostrophe
    content = content.replace("\\\\'", "\\'")
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed apostrophes in all strings.xml")
