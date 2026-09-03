#!/usr/bin/env python3
"""
generate_mock_epubs.py
Generates 5 authentic, fully-compliant EPUB 3.0 e-book files with rich styling,
table of contents, cover art, and diverse genre content.
"""
import sys
import os
import io
import zipfile
import uuid
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont

sys.stdout.reconfigure(encoding='utf-8')

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "mock_epubs")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def create_cover_image(title, subtitle, author, bg_color, accent_color, width=600, height=800):
    """Generates an aesthetic book cover image using PIL."""
    img = Image.new("RGB", (width, height), bg_color)
    draw = ImageDraw.Draw(img)

    # Decorative border frame
    draw.rectangle([(24, 24), (width - 24, height - 24)], outline=accent_color, width=3)
    draw.rectangle([(32, 32), (width - 32, height - 32)], outline=accent_color, width=1)

    # Header decorative pill / category badge
    badge_text = "PDFCHEMY CLASSIC EDITIONS"
    draw.rectangle([(width // 2 - 140, 60), (width // 2 + 140, 90)], fill=accent_color)
    draw.text((width // 2, 75), badge_text, fill=bg_color, anchor="mm")

    # Large title (simulated multiline)
    words = title.split()
    lines = []
    curr = []
    for w in words:
        curr.append(w)
        if len(" ".join(curr)) > 16:
            lines.append(" ".join(curr[:-1]))
            curr = [w]
    if curr:
        lines.append(" ".join(curr))

    y_pos = 260
    for line in lines:
        draw.text((width // 2, y_pos), line, fill=(255, 255, 255), anchor="mm")
        y_pos += 45

    # Subtitle
    draw.line([(width // 2 - 60, y_pos + 10), (width // 2 + 60, y_pos + 10)], fill=accent_color, width=2)
    y_pos += 40
    draw.text((width // 2, y_pos), subtitle, fill=(210, 215, 225), anchor="mm")

    # Author near bottom
    draw.text((width // 2, height - 120), "BY", fill=accent_color, anchor="mm")
    draw.text((width // 2, height - 85), author.upper(), fill=(255, 255, 255), anchor="mm")

    # Save to JPEG bytes
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()

BASE_CSS = """
@charset "utf-8";
body {
    margin: 5% 8%;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1a1a1a;
    background-color: #ffffff;
    line-height: 1.6;
    font-size: 1.05em;
}
h1, h2, h3 {
    color: #0f172a;
    font-weight: 700;
    margin-top: 1.5em;
    margin-bottom: 0.5em;
    line-height: 1.25;
}
h1 {
    font-size: 2em;
    border-bottom: 2px solid #0284c7;
    padding-bottom: 0.3em;
}
h2 {
    font-size: 1.5em;
    color: #0369a1;
}
p {
    margin-top: 0;
    margin-bottom: 1.2em;
    text-align: justify;
}
.lead {
    font-size: 1.2em;
    font-weight: 500;
    color: #334155;
}
.dropcap {
    float: left;
    font-size: 3.4em;
    line-height: 0.8;
    margin: 0.1em 0.15em 0 0;
    color: #0284c7;
    font-weight: bold;
}
blockquote {
    border-left: 4px solid #0284c7;
    margin: 1.5em 0;
    padding: 0.8em 1.2em;
    background: #f0f9ff;
    color: #0369a1;
    font-style: italic;
}
pre, code {
    font-family: "Courier New", Courier, monospace;
    background: #f1f5f9;
    color: #0f172a;
    border-radius: 4px;
}
code {
    padding: 2px 6px;
    font-size: 0.9em;
}
pre {
    padding: 12px 16px;
    overflow-x: auto;
    border: 1px solid #cbd5e1;
    font-size: 0.85em;
    line-height: 1.4;
}
table {
    width: 100%;
    border-collapse: collapse;
    margin: 1.5em 0;
}
th, td {
    border: 1px solid #cbd5e1;
    padding: 10px 14px;
    text-align: left;
}
th {
    background-color: #f8fafc;
    font-weight: 600;
    color: #0f172a;
}
tr:nth-child(even) {
    background-color: #f1f5f9;
}
.callout {
    border: 1px solid #bae6fd;
    background: #f0f9ff;
    padding: 14px 18px;
    border-radius: 6px;
    margin: 1.5em 0;
}
.callout-title {
    font-weight: bold;
    color: #0369a1;
    margin-bottom: 6px;
}
.cover-img {
    width: 100%;
    height: auto;
    max-height: 95vh;
    object-fit: contain;
    display: block;
    margin: 0 auto;
}
"""

def build_epub_file(filepath, book_data):
    """Packages book chapters, images, and metadata into a valid EPUB 3.0 zip."""
    book_id = str(uuid.uuid4())
    now_str = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    
    # Generate cover image
    cover_bytes = create_cover_image(
        book_data["title"],
        book_data.get("subtitle", "Edition 2026"),
        book_data["author"],
        book_data["bg_color"],
        book_data["accent_color"]
    )

    with zipfile.ZipFile(filepath, "w") as z:
        # 1. mimetype (MUST be first entry, uncompressed)
        z.writestr("mimetype", "application/epub+zip", compress_type=zipfile.ZIP_STORED)

        # 2. META-INF/container.xml
        container_xml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
    <rootfiles>
        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
    </rootfiles>
</container>"""
        z.writestr("META-INF/container.xml", container_xml, compress_type=zipfile.ZIP_DEFLATED)

        # 3. OEBPS/style.css
        z.writestr("OEBPS/style.css", BASE_CSS, compress_type=zipfile.ZIP_DEFLATED)

        # 4. Cover image
        z.writestr("OEBPS/cover.jpg", cover_bytes, compress_type=zipfile.ZIP_DEFLATED)

        # 5. Cover XHTML
        cover_xhtml = f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
    <title>Cover - {book_data['title']}</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body style="margin:0; padding:0; text-align:center;">
    <img src="cover.jpg" alt="Book Cover" class="cover-img"/>
</body>
</html>"""
        z.writestr("OEBPS/cover.xhtml", cover_xhtml, compress_type=zipfile.ZIP_DEFLATED)

        # 6. Chapters
        manifest_items = [
            '<item id="style" href="style.css" media-type="text/css"/>',
            '<item id="cover-image" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>',
            '<item id="cover" href="cover.xhtml" media-type="application/xhtml+xml"/>',
            '<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>',
            '<item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>'
        ]
        spine_items = [
            '<itemref idref="cover"/>'
        ]
        nav_toc_links = []
        ncx_navpoints = []

        for idx, ch in enumerate(book_data["chapters"], start=1):
            ch_id = f"ch_{idx}"
            ch_filename = f"chapter_{idx}.xhtml"
            manifest_items.append(f'<item id="{ch_id}" href="{ch_filename}" media-type="application/xhtml+xml"/>')
            spine_items.append(f'<itemref idref="{ch_id}"/>')
            nav_toc_links.append(f'<li><a href="{ch_filename}">{ch["title"]}</a></li>')
            ncx_navpoints.append(f"""    <navPoint id="np_{idx}" playOrder="{idx}">
        <navLabel><text>{ch["title"]}</text></navLabel>
        <content src="{ch_filename}"/>
    </navPoint>""")

            ch_xhtml = f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
    <title>{ch['title']}</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
    <h1>{ch['title']}</h1>
    {ch['content']}
</body>
</html>"""
            z.writestr(f"OEBPS/{ch_filename}", ch_xhtml, compress_type=zipfile.ZIP_DEFLATED)

        # 7. Navigation (EPUB 3 nav.xhtml)
        nav_xhtml = f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
    <title>Table of Contents</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
    <nav epub:type="toc" id="toc">
        <h1>Table of Contents</h1>
        <ol>
            {''.join(nav_toc_links)}
        </ol>
    </nav>
</body>
</html>"""
        z.writestr("OEBPS/nav.xhtml", nav_xhtml, compress_type=zipfile.ZIP_DEFLATED)

        # 8. NCX (EPUB 2 toc.ncx)
        toc_ncx = f"""<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
    <head>
        <meta name="dtb:uid" content="urn:uuid:{book_id}"/>
        <meta name="dtb:depth" content="1"/>
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
    </head>
    <docTitle><text>{book_data['title']}</text></docTitle>
    <docAuthor><text>{book_data['author']}</text></docAuthor>
    <navMap>
{''.join(ncx_navpoints)}
    </navMap>
</ncx>"""
        z.writestr("OEBPS/toc.ncx", toc_ncx, compress_type=zipfile.ZIP_DEFLATED)

        # 9. content.opf
        content_opf = f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="3.0">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
        <dc:identifier id="BookId">urn:uuid:{book_id}</dc:identifier>
        <dc:title>{book_data['title']}</dc:title>
        <dc:creator>{book_data['author']}</dc:creator>
        <dc:language>en</dc:language>
        <dc:publisher>PDFchemy Press</dc:publisher>
        <dc:description>{book_data.get('description', '')}</dc:description>
        <meta property="dcterms:modified">{now_str}</meta>
    </metadata>
    <manifest>
        {''.join(manifest_items)}
    </manifest>
    <spine toc="ncx">
        {''.join(spine_items)}
    </spine>
</package>"""
        z.writestr("OEBPS/content.opf", content_opf, compress_type=zipfile.ZIP_DEFLATED)

    print(f"  [OK] Generated {os.path.basename(filepath)} ({os.path.getsize(filepath) / 1024:.1f} KB)")

# ==============================================================================
# 5 BOOK DEFINITIONS
# ==============================================================================

BOOKS = [
    # 1. Classic Literature
    {
        "filename": "01_Pride_and_Prejudice_Classic_Novel.epub",
        "title": "Pride and Prejudice",
        "subtitle": "A Romance of Manners",
        "author": "Jane Austen",
        "bg_color": (30, 20, 25),
        "accent_color": (212, 160, 100),
        "description": "The timeless 1813 masterpiece exploring love, reputation, and class in Regency England.",
        "chapters": [
            {
                "title": "Chapter I",
                "content": """
<p><span class="dropcap">I</span>t is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.</p>
<p>However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.</p>
<blockquote>"My dear Mr. Bennet," said his lady to him one day, "have you heard that Netherfield Park is let at last?"</blockquote>
<p>Mr. Bennet replied that he had not.</p>
<p>"But it is," returned she; "for Mrs. Long has just been here, and she told me all about it." Mr. Bennet made no answer.</p>
<p>"Do you not want to know who has taken it?" cried his wife impatiently.</p>
<p>"You want to tell me, and I have no objection to hearing it."</p>
<p>This was invitation enough. "Why, my dear, you must know, Mrs. Long says that Netherfield is taken by a young man of large fortune from the north of England; that he came down on Monday in a chaise and four to see the place, and was so much delighted with it, that he agreed with Mr. Morris immediately."</p>
"""
            },
            {
                "title": "Chapter II",
                "content": """
<p><span class="dropcap">M</span>r. Bennet was among the earliest of those who waited on Mr. Bingley. He had always intended to visit him, though to the last always assuring his wife that he should not go; and till the evening after the visit was paid she had no knowledge of it.</p>
<p>The rest of the evening was spent in conjecturing how soon he would return Mr. Bennet's visit, and determining when they should ask him to dinner.</p>
<div class="callout">
    <div class="callout-title">Historical Note on Regency Etiquette</div>
    Gentlemen were required by custom to pay formal morning calls before any social invitation or ball introduction could be officially extended.
</div>
"""
            },
            {
                "title": "Chapter III",
                "content": """
<p><span class="dropcap">N</span>ot all that Mrs. Bennet, however, with the assistance of her five daughters, could ask on the subject, was sufficient to draw from her husband any satisfactory description of Mr. Bingley.</p>
<p>Mr. Bingley was good-looking and gentlemanlike; he had a pleasant countenance, and easy, unaffected manners. His sisters were fine women, with an air of decided fashion. His brother-in-law, Mr. Hurst, merely looked the gentleman; but his friend Mr. Darcy soon drew the attention of the room by his fine, tall person, handsome features, noble mien, and the report which was in general circulation within five minutes after his entrance, of his having ten thousand a year.</p>
"""
            }
        ]
    },

    # 2. Programming / Technical
    {
        "filename": "02_Python_Data_Science_Cookbook.epub",
        "title": "Python Data Science Cookbook",
        "subtitle": "Modern Recipes for Data Wrangling",
        "author": "Dr. Elena Vance",
        "bg_color": (15, 23, 42),
        "accent_color": (56, 189, 248),
        "description": "A hands-on reference guide containing real-world recipes for pandas, NumPy, and statistical inference.",
        "chapters": [
            {
                "title": "Chapter 1: Memory-Efficient DataFrame Operations",
                "content": """
<p class="lead">Processing multi-gigabyte datasets on local machines requires conscious memory management and downcasting numeric datatypes.</p>
<h2>1.1 Profiling Memory Footprint</h2>
<p>Before optimizing your pandas pipeline, inspect memory consumption down to deep object inspection:</p>
<pre><code>import pandas as pd

def reduce_mem_usage(df: pd.DataFrame) -> pd.DataFrame:
    start_mem = df.memory_usage(deep=True).sum() / 1024**2
    for col in df.columns:
        col_type = df[col].dtype
        if col_type != object:
            c_min, c_max = df[col].min(), df[col].max()
            if str(col_type)[:3] == 'int':
                if c_min > np.iinfo(np.int8).min and c_max < np.iinfo(np.int8).max:
                    df[col] = df[col].astype(np.int8)
                elif c_min > np.iinfo(np.int16).min and c_max < np.iinfo(np.int16).max:
                    df[col] = df[col].astype(np.int16)
    end_mem = df.memory_usage(deep=True).sum() / 1024**2
    print(f"Memory decreased to {end_mem:.2f} MB ({100 * (start_mem - end_mem) / start_mem:.1f}% reduction)")
    return df</code></pre>
<div class="callout">
    <div class="callout-title">Pro Tip: PyArrow Backends</div>
    As of pandas 2.0+, specifying <code>engine='pyarrow'</code> or <code>dtype_backend='pyarrow'</code> dramatically speeds up string manipulation and cuts memory footprint by up to 60%.
</div>
"""
            },
            {
                "title": "Chapter 2: Fast Vectorized Aggregations",
                "content": """
<h2>2.1 Benchmark Comparisons</h2>
<p>The following table illustrates relative benchmark times across standard dataframe operations on 10 million rows:</p>
<table>
    <thead>
        <tr>
            <th>Operation</th>
            <th>Pure Python Loop</th>
            <th>Pandas Apply</th>
            <th>Vectorized NumPy</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Log Transform</td>
            <td>1,420 ms</td>
            <td>380 ms</td>
            <td>12 ms</td>
        </tr>
        <tr>
            <td>Conditional Filter</td>
            <td>980 ms</td>
            <td>210 ms</td>
            <td>8 ms</td>
        </tr>
        <tr>
            <td>Euclidean Distance</td>
            <td>2,340 ms</td>
            <td>640 ms</td>
            <td>24 ms</td>
        </tr>
    </tbody>
</table>
<p>Always prioritize NumPy vectorization or Numba JIT compilation when operating over column arrays.</p>
"""
            }
        ]
    },

    # 3. Cybersecurity / Infosec Field Guide
    {
        "filename": "03_Cybersecurity_Incident_Response_Field_Guide.epub",
        "title": "Incident Response Field Guide",
        "subtitle": "Emergency Protocols & Forensic Triage",
        "author": "Marcus Sterling, CISSP",
        "bg_color": (20, 10, 30),
        "accent_color": (239, 68, 68),
        "description": "Tactical triage protocols, containment commands, and forensic evidence collection for SOC teams.",
        "chapters": [
            {
                "title": "Phase 1: Initial Discovery & Severity Scoring",
                "content": """
<p class="lead">The first 15 minutes of suspected adversary intrusion dictate whether a containment window remains viable.</p>
<h2>1. Incident Severity Matrix</h2>
<table>
    <thead>
        <tr>
            <th>Severity Tier</th>
            <th>Indicator Threshold</th>
            <th>Action SLA</th>
            <th>Escalation Path</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><strong>SEV-1 (Critical)</strong></td>
            <td>Active Domain Controller compromise or ransomware propagation</td>
            <td>&lt; 15 Minutes</td>
            <td>CISO, Executive Leadership, Legal Counsel</td>
        </tr>
        <tr>
            <td><strong>SEV-2 (High)</strong></td>
            <td>Privileged credential theft or C2 beaconing on sensitive servers</td>
            <td>&lt; 1 Hour</td>
            <td>SOC Lead, Infrastructure Engineering</td>
        </tr>
        <tr>
            <td><strong>SEV-3 (Medium)</strong></td>
            <td>Isolated workstation malware or suspicious phishing submission</td>
            <td>&lt; 4 Hours</td>
            <td>Assigned IR Analyst</td>
        </tr>
    </tbody>
</table>
"""
            },
            {
                "title": "Phase 2: Live Memory Preservation",
                "content": """
<h2>Volatile Evidence Preservation Checklist</h2>
<p>Prior to pulling power or executing hypervisor freeze commands, capture volatile memory artifacts:</p>
<pre><code># 1. Dump RAM to write-blocked external storage
winpmem.exe -o E:\\forensics\\evidence_01.raw

# 2. Record open network sockets and PID connections
netstat -ano > E:\\forensics\\netstat_active.txt

# 3. Capture process tree with full arguments
wmic process get processid,parentprocessid,executablepath,commandline /format:csv > E:\\forensics\\processes.csv</code></pre>
<div class="callout">
    <div class="callout-title">CRITICAL EVIDENCE WARNING</div>
    Never perform system reboots or run untrusted local utilities on an active victim host. Memory-only malware will evaporate upon reboot.
</div>
"""
            }
        ]
    },

    # 4. Illustrated Culinary
    {
        "filename": "04_The_Art_of_Japanese_Cooking_Illustrated.epub",
        "title": "The Art of Japanese Cooking",
        "subtitle": "Traditional Flavors & Modern Techniques",
        "author": "Chef Kenji Sato",
        "bg_color": (24, 20, 15),
        "accent_color": (234, 179, 8),
        "description": "Master the fundamental pillars of Washoku: balance, seasonal umami, and pristine knife craftsmanship.",
        "chapters": [
            {
                "title": "Chapter 1: The Essence of Dashi",
                "content": """
<p><span class="dropcap">D</span>ashi represents the beating heart of Japanese gastronomy. Unlike Western stocks simmered for hours, traditional dashi is crafted in mere minutes through the gentle extraction of glutamic and inosinic acids.</p>
<h2>Standard Ichiban Dashi (First Brew)</h2>
<p><strong>Ingredients:</strong></p>
<ul>
    <li>1 Liter filtered spring water</li>
    <li>20g Rishiri or Ma-kombu kelp</li>
    <li>25g Shaved Katsuobushi (aged bonito flakes)</li>
</ul>
<p><strong>Method:</strong></p>
<ol>
    <li>Gently wipe the kombu surface with a damp cloth to remove grit without stripping the natural white mannitol crystals.</li>
    <li>Submerge kombu in cold water for 1 hour. Place over medium-low flame.</li>
    <li>Just before bubbles break into a vigorous boil (approximately 80°C), remove the kelp immediately to avoid bitter extraction.</li>
    <li>Turn off the flame, add the katsuobushi flakes, and allow them to sink over 3 minutes.</li>
    <li>Strain through a fine mesh lined with unbleached cotton. Do not press the solids.</li>
</ol>
<blockquote>"A single sip of true dashi clarifies the mind and awakens the palate for the feast ahead."</blockquote>
"""
            },
            {
                "title": "Chapter 2: Perfect Sushi Rice (Shari)",
                "content": """
<h2>The Golden Ratio of Awase-zu (Vinegar Seasoning)</h2>
<table>
    <thead>
        <tr>
            <th>Seasoning Ingredient</th>
            <th>Standard Ratio</th>
            <th>Notes</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Rice Vinegar (Komezu)</td>
            <td>100 ml</td>
            <td>Opt for brewed Junmai rice vinegar</td>
        </tr>
        <tr>
            <td>Sugar</td>
            <td>40 g</td>
            <td>Balances acidity</td>
        </tr>
        <tr>
            <td>Sea Salt</td>
            <td>18 g</td>
            <td>Enhances grain sweetness</td>
        </tr>
    </tbody>
</table>
<p>Cut the seasoning into freshly steamed short-grain rice with diagonal slicing motions using a wooden shamoji while gently fanning with a uchiwa to impart a glossy luster.</p>
"""
            }
        ]
    },

    # 5. Startup / Business Handbook
    {
        "filename": "05_Startup_Founders_Handbook_MultiChapter.epub",
        "title": "Startup Founder's Handbook",
        "subtitle": "From Zero to Product-Market Fit",
        "author": "Sophia Lin",
        "bg_color": (15, 30, 25),
        "accent_color": (34, 197, 94),
        "description": "Actionable blueprints on unit economics, retention cohorts, hiring founding engineers, and Seed/Series A fundraising.",
        "chapters": [
            {
                "title": "Chapter 1: The Anatomy of Retention",
                "content": """
<p class="lead">If your 30-day retention curve does not flatten parallel to the x-axis, you do not have product-market fit. Growth tactics poured into a leaking bucket are pure burn.</p>
<h2>1. Cohort Analysis Framework</h2>
<table>
    <thead>
        <tr>
            <th>Product Category</th>
            <th>Benchmark D30 Retention</th>
            <th>World-Class D30 Retention</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>B2B SaaS Enterprise</td>
            <td>85% - 90% Net Dollar</td>
            <td>120%+ Net Retention</td>
        </tr>
        <tr>
            <td>Consumer Mobile App</td>
            <td>15% - 20%</td>
            <td>35%+ Flat Baseline</td>
        </tr>
        <tr>
            <td>Utility & Document Tools</td>
            <td>25% - 30%</td>
            <td>45%+ Monthly Active Return</td>
        </tr>
    </tbody>
</table>
<div class="callout">
    <div class="callout-title">The Local-First Advantage</div>
    Utility products that offer zero-login, instant on-device value exhibit 2.4x higher initial task completion rates compared to cloud-mandatory registration gates.
</div>
"""
            },
            {
                "title": "Chapter 2: Fundraising & Capital Efficiency",
                "content": """
<h2>The Seed Round Equation</h2>
<p>When presenting financial models to early-stage venture partners, anchor on realistic CAC-to-LTV ratios:</p>
<blockquote>"Growth is a vanity metric; unit economics are sanity; cash flow is reality."</blockquote>
<ul>
    <li><strong>LTV / CAC Ratio:</strong> Must exceed 3.0x over a 12-month trailing cohort.</li>
    <li><strong>CAC Payback Period:</strong> Aim for under 8 months on organic/hybrid channels.</li>
    <li><strong>Rule of 40:</strong> Annual Growth Rate + Free Cash Flow Margin should surpass 40%.</li>
</ul>
<p>Consistently demonstrate capital discipline by automating repetitive workflows and maintaining lean operational footprints.</p>
"""
            }
        ]
    }
]

def main():
    print("==================================================")
    print("  PDFCHEMY TOOLS: GENERATING 5 MOCK EPUB E-BOOKS")
    print("==================================================")

    for b in BOOKS:
        dest = os.path.join(OUTPUT_DIR, b["filename"])
        build_epub_file(dest, b)

    print("\nAll 5 EPUB mockup files successfully created in:")
    print(f"  {OUTPUT_DIR}")

if __name__ == "__main__":
    main()
