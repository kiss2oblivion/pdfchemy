import os

UI_DIR = r"c:\Users\cucos\AndroidStudioProjects\Shrinkpdf\app\src\main\java\com\example\shrinkpdf\ui"
MAIN_ACTIVITY = r"c:\Users\cucos\AndroidStudioProjects\Shrinkpdf\app\src\main\java\com\example\shrinkpdf\MainActivity.kt"

IMPORTS = """import androidx.compose.ui.res.stringResource
import com.example.shrinkpdf.R
"""

def add_imports(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if "import androidx.compose.ui.res.stringResource" not in content:
        # find last import
        lines = content.split('\n')
        last_import_idx = -1
        for i, line in enumerate(lines):
            if line.startswith('import '):
                last_import_idx = i
        
        if last_import_idx != -1:
            lines.insert(last_import_idx + 1, IMPORTS.strip())
            new_content = '\n'.join(lines)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Added imports to {os.path.basename(filepath)}")

for root, dirs, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            add_imports(os.path.join(root, file))

add_imports(MAIN_ACTIVITY)
