import os
import subprocess
import json
import urllib.request
import re

# We will install markdown if not present
try:
    import markdown
except ImportError:
    subprocess.check_call(["pip", "install", "markdown"])
    import markdown

MD_FILE = r"C:\Users\cucos\.gemini\antigravity-ide\brain\3e4405cc-c689-4be6-95a3-916b12932baf\privacy_policy.md"
SITE_DIR = r"website"
PUB_DIR = os.path.join(SITE_DIR, "public")

os.makedirs(PUB_DIR, exist_ok=True)

with open(MD_FILE, "r", encoding="utf-8") as f:
    md_text = f.read()

html_content = markdown.markdown(md_text)

full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Privacy Policy - PDFchemy</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 800px;
            margin: 0 auto;
            padding: 2rem;
            background-color: #f9f9f9;
        }}
        .container {{
            background-color: #fff;
            padding: 2rem 3rem;
            border-radius: 8px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
        }}
        h1, h2, h3 {{ color: #2c3e50; }}
        h1 {{ border-bottom: 2px solid #eee; padding-bottom: 0.5rem; }}
        a {{ color: #3498db; text-decoration: none; }}
        a:hover {{ text-decoration: underline; }}
    </style>
</head>
<body>
    <div class="container">
        {html_content}
    </div>
</body>
</html>
"""

with open(os.path.join(PUB_DIR, "privacy.html"), "w", encoding="utf-8") as f:
    f.write(full_html)

# Create index.html redirecting to privacy.html
with open(os.path.join(PUB_DIR, "index.html"), "w", encoding="utf-8") as f:
    f.write('<meta http-equiv="refresh" content="0; url=/privacy.html" />')

# Write .firebaserc
with open(os.path.join(SITE_DIR, ".firebaserc"), "w") as f:
    json.dump({
      "projects": {
        "default": "pdfchemy-tools"
      }
    }, f, indent=2)

# Write firebase.json
with open(os.path.join(SITE_DIR, "firebase.json"), "w") as f:
    json.dump({
      "hosting": {
        "public": "public",
        "ignore": [
          "firebase.json",
          "**/.*",
          "**/node_modules/**"
        ]
      }
    }, f, indent=2)

print("Files generated. Deploying...")
res = subprocess.run(["npx", "-y", "firebase-tools", "deploy", "--only", "hosting"], cwd=SITE_DIR, capture_output=True, text=True, shell=True)
print(res.stdout)
if res.stderr:
    print("STDERR:", res.stderr)
