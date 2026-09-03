import os
import sys
import fitz  # PyMuPDF
import math

sys.stdout.reconfigure(encoding='utf-8')

OUTPUT_DIR = os.path.abspath("mock_pdfs")
os.makedirs(OUTPUT_DIR, exist_ok=True)

print(f"Generating 5 realistic PDF mockups into: {OUTPUT_DIR}")

# -------------------------------------------------------------
# 1. CONTRACT & SERVICE AGREEMENT
# -------------------------------------------------------------
def generate_contract():
    doc = fitz.open()
    
    # Page 1
    page = doc.new_page(width=595, height=842) # A4
    rect = fitz.Rect(50, 50, 545, 792)
    
    # Header Banner
    page.draw_rect(fitz.Rect(50, 45, 545, 100), color=None, fill=(0.08, 0.24, 0.45))
    page.insert_text(fitz.Point(65, 80), "APEX GLOBAL ENTERPRISES", fontsize=18, color=(1, 1, 1), fontname="helv")
    page.insert_text(fitz.Point(65, 93), "MASTER SERVICES AGREEMENT - CONFIDENTIAL", fontsize=9, color=(0.8, 0.9, 1), fontname="helv")
    
    body = (
        "This Master Services Agreement ('Agreement') is made and entered into as of September 3, 2026, "
        "by and between Apex Global Enterprises LLC ('Company') and Quantum Dynamics Inc ('Client').\n\n"
        "1. SCOPE OF ENGAGEMENT\n"
        "Company shall perform enterprise software optimization, cloud infrastructure hardening, and local-first "
        "document privacy compliance services in accordance with Statement of Work #2026-B.\n\n"
        "2. COMPENSATION AND PAYMENT TERMS\n"
        "Client agrees to remit payment within thirty (30) calendar days of invoice date. Late payments shall incur "
        "a statutory surcharge of 1.5% per month or the maximum permissible by law.\n\n"
        "3. CONFIDENTIAL INFORMATION & TRADE SECRETS\n"
        "Each party agrees to maintain strict confidentiality of all proprietary source code, cryptographic keys, "
        "and client records. Under no circumstance shall customer unencrypted files leave the local device runtime.\n\n"
        "4. INTELLECTUAL PROPERTY INDEMNIFICATION\n"
        "All work product, custom modules, and algorithmic implementations shall vest entirely in Client upon "
        "full satisfaction of outstanding balances."
    )
    page.insert_textbox(fitz.Rect(50, 120, 545, 550), body, fontsize=10.5, fontname="times-roman", lineheight=1.4)
    
    # Signatures
    page.draw_line(fitz.Point(50, 680), fitz.Point(260, 680), color=(0.2, 0.2, 0.2), width=1)
    page.insert_text(fitz.Point(50, 695), "Authorized Signature: Johnathan Vance", fontsize=9, fontname="helv")
    page.insert_text(fitz.Point(50, 710), "Title: Chief Technology Officer", fontsize=8, color=(0.4, 0.4, 0.4), fontname="helv")
    page.insert_text(fitz.Point(50, 725), "Date: September 03, 2026", fontsize=8, color=(0.4, 0.4, 0.4), fontname="helv")

    page.draw_line(fitz.Point(320, 680), fitz.Point(530, 680), color=(0.2, 0.2, 0.2), width=1)
    page.insert_text(fitz.Point(320, 695), "Client Representative: Elena Rostova", fontsize=9, fontname="helv")
    page.insert_text(fitz.Point(320, 710), "Title: VP of Legal & Compliance", fontsize=8, color=(0.4, 0.4, 0.4), fontname="helv")
    page.insert_text(fitz.Point(320, 725), "Date: September 03, 2026", fontsize=8, color=(0.4, 0.4, 0.4), fontname="helv")
    
    # Verification Stamp Box
    page.draw_rect(fitz.Rect(350, 540, 520, 620), color=(0.7, 0.1, 0.1), width=1.5)
    page.insert_text(fitz.Point(370, 570), "LEGAL REVIEW", fontsize=12, color=(0.7, 0.1, 0.1), fontname="helv")
    page.insert_text(fitz.Point(365, 590), "APPROVED FOR EXECUTION", fontsize=8, color=(0.7, 0.1, 0.1), fontname="helv")
    page.insert_text(fitz.Point(385, 605), "REF: 2026-MS-994", fontsize=7, color=(0.7, 0.1, 0.1), fontname="helv")
    
    # Footer
    page.draw_line(fitz.Point(50, 770), fitz.Point(545, 770), color=(0.8, 0.8, 0.8), width=0.5)
    page.insert_text(fitz.Point(50, 785), "Apex Global Enterprises • 100% Confidential • Document ID: #AG-2026-8812", fontsize=7.5, color=(0.5, 0.5, 0.5), fontname="helv")
    page.insert_text(fitz.Point(500, 785), "Page 1 of 1", fontsize=7.5, color=(0.5, 0.5, 0.5), fontname="helv")
    
    path = os.path.join(OUTPUT_DIR, "01_Contract_Agreement_Signed.pdf")
    doc.save(path)
    print(f"✓ Saved: {path}")

# -------------------------------------------------------------
# 2. COMMERCIAL TAX INVOICE WITH FILLABLE ACROFORMS
# -------------------------------------------------------------
def generate_invoice():
    doc = fitz.open()
    page = doc.new_page(width=595, height=842)
    
    # Top Header
    page.insert_text(fitz.Point(50, 75), "INVOICE", fontsize=28, color=(0.1, 0.2, 0.35), fontname="helv")
    page.insert_text(fitz.Point(50, 95), "PDFCHEMY CLOUD & SYSTEM SERVICES", fontsize=9, color=(0.4, 0.4, 0.4), fontname="helv")
    
    page.insert_text(fitz.Point(400, 60), "Invoice #: INV-2026-1049", fontsize=10, fontname="helv")
    page.insert_text(fitz.Point(400, 75), "Date: September 03, 2026", fontsize=9, color=(0.4, 0.4, 0.4), fontname="helv")
    page.insert_text(fitz.Point(400, 90), "Due Date: October 03, 2026", fontsize=9, color=(0.4, 0.4, 0.4), fontname="helv")
    
    page.draw_line(fitz.Point(50, 110), fitz.Point(545, 110), color=(0.85, 0.85, 0.85), width=1)
    
    # Bill To
    page.insert_text(fitz.Point(50, 135), "BILLED TO:", fontsize=10, color=(0.2, 0.2, 0.2), fontname="helv")
    page.insert_text(fitz.Point(50, 155), "Acme Global Industries LLC", fontsize=10, fontname="helv")
    page.insert_text(fitz.Point(50, 170), "742 Evergreen Terrace, Suite 400", fontsize=9, color=(0.4, 0.4, 0.4), fontname="helv")
    page.insert_text(fitz.Point(50, 185), "Springfield, OR 97477 • contact@acmeglobal.com", fontsize=9, color=(0.4, 0.4, 0.4), fontname="helv")
    
    # Table Header
    page.draw_rect(fitz.Rect(50, 210, 545, 235), color=None, fill=(0.93, 0.95, 0.98))
    page.insert_text(fitz.Point(60, 227), "DESCRIPTION", fontsize=9, color=(0.2, 0.3, 0.4), fontname="helv")
    page.insert_text(fitz.Point(340, 227), "HOURS / QTY", fontsize=9, color=(0.2, 0.3, 0.4), fontname="helv")
    page.insert_text(fitz.Point(430, 227), "RATE", fontsize=9, color=(0.2, 0.3, 0.4), fontname="helv")
    page.insert_text(fitz.Point(495, 227), "AMOUNT", fontsize=9, color=(0.2, 0.3, 0.4), fontname="helv")
    
    items = [
        ("Offline PDF Engine Security Audit & Optimization", "40 hrs", "$150.00", "$6,000.00"),
        ("Android Compose Multiplatform Architecture Refactor", "32 hrs", "$160.00", "$5,120.00"),
        ("ML Kit OCR & AcroForm Parser Implementation", "25 hrs", "$175.00", "$4,375.00"),
        ("Store Metadata Localization & ASO Deployment (24 Locales)", "12 hrs", "$120.00", "$1,440.00"),
        ("Enterprise Annual Support & Bug Warranty", "1 unit", "$2,500.00", "$2,500.00")
    ]
    
    y = 260
    for desc, qty, rate, amt in items:
        page.insert_text(fitz.Point(60, y), desc, fontsize=9.5, fontname="helv")
        page.insert_text(fitz.Point(350, y), qty, fontsize=9.5, fontname="helv")
        page.insert_text(fitz.Point(430, y), rate, fontsize=9.5, fontname="helv")
        page.insert_text(fitz.Point(490, y), amt, fontsize=9.5, fontname="helv")
        page.draw_line(fitz.Point(50, y + 10), fitz.Point(545, y + 10), color=(0.92, 0.92, 0.92), width=0.5)
        y += 30
        
    # Totals
    page.insert_text(fitz.Point(390, y + 20), "Subtotal:", fontsize=10, fontname="helv")
    page.insert_text(fitz.Point(485, y + 20), "$19,435.00", fontsize=10, fontname="helv")
    page.insert_text(fitz.Point(390, y + 40), "Tax (0.0%):", fontsize=10, color=(0.4, 0.4, 0.4), fontname="helv")
    page.insert_text(fitz.Point(485, y + 40), "$0.00", fontsize=10, color=(0.4, 0.4, 0.4), fontname="helv")
    
    page.draw_rect(fitz.Rect(380, y + 55, 545, y + 85), color=None, fill=(0.1, 0.2, 0.35))
    page.insert_text(fitz.Point(390, y + 75), "TOTAL DUE:", fontsize=11, color=(1, 1, 1), fontname="helv")
    page.insert_text(fitz.Point(475, y + 75), "$19,435.00", fontsize=12, color=(1, 1, 1), fontname="helv")
    
    # Interactive AcroForm Fields for testing form filling!
    # Field 1: Customer PO
    page.insert_text(fitz.Point(50, 520), "Purchase Order # (Editable Field):", fontsize=9, fontname="helv")
    widget1 = fitz.Widget()
    widget1.rect = fitz.Rect(50, 530, 250, 555)
    widget1.field_type = fitz.PDF_WIDGET_TYPE_TEXT
    widget1.field_name = "PurchaseOrderNumber"
    widget1.field_value = "PO-99201"
    widget1.text_color = (0, 0, 0)
    widget1.fill_color = (0.95, 0.97, 1.0)
    widget1.border_color = (0.4, 0.6, 0.9)
    page.add_widget(widget1)
    
    # Field 2: Client Notes
    page.insert_text(fitz.Point(50, 580), "Approver Notes (Editable Field):", fontsize=9, fontname="helv")
    widget2 = fitz.Widget()
    widget2.rect = fitz.Rect(50, 590, 545, 650)
    widget2.field_type = fitz.PDF_WIDGET_TYPE_TEXT
    widget2.field_name = "ApproverNotes"
    widget2.field_value = "Approved for corporate wire transfer on 09/04/2026."
    widget2.text_color = (0, 0, 0)
    widget2.fill_color = (0.95, 0.97, 1.0)
    widget2.border_color = (0.4, 0.6, 0.9)
    page.add_widget(widget2)
    
    # Checkbox Field
    page.insert_text(fitz.Point(75, 680), "Confirm tax exemption certificate attached", fontsize=9, fontname="helv")
    widget3 = fitz.Widget()
    widget3.rect = fitz.Rect(50, 670, 68, 688)
    widget3.field_type = fitz.PDF_WIDGET_TYPE_CHECKBOX
    widget3.field_name = "TaxExemptConfirmed"
    widget3.field_value = "Yes"
    widget3.border_color = (0.2, 0.4, 0.8)
    page.add_widget(widget3)
    
    # Footer
    page.draw_line(fitz.Point(50, 780), fitz.Point(545, 780), color=(0.85, 0.85, 0.85), width=0.5)
    page.insert_text(fitz.Point(50, 795), "Payment Details: Wire Transfer Routing #021000021 • Account #8839201994", fontsize=8, color=(0.5, 0.5, 0.5), fontname="helv")
    
    path = os.path.join(OUTPUT_DIR, "02_Business_Invoice_Fillable.pdf")
    doc.save(path)
    print(f"✓ Saved: {path}")

# -------------------------------------------------------------
# 3. HIGH-RESOLUTION PRODUCT CATALOG (FOR COMPRESSION TESTING)
# -------------------------------------------------------------
def generate_product_catalog():
    doc = fitz.open()
    
    # Generate high resolution colorful synthetic images to give compression engine something real to crunch
    import io
    from PIL import Image, ImageDraw
    
    images = []
    for i in range(3):
        # Create a large 1600x1200 RGB image with gradient, geometric shapes and textures
        img = Image.new("RGB", (1600, 1200), color=(240, 245, 250))
        draw = ImageDraw.Draw(img)
        # Background gradient bands
        for y_step in range(0, 1200, 20):
            r = int(30 + (y_step / 1200) * 120)
            g = int(70 + (y_step / 1200) * 80)
            b = int(140 + (y_step / 1200) * 100)
            draw.rectangle([0, y_step, 1600, y_step + 20], fill=(r, g, b))
        # Circles & shapes
        for c in range(12):
            cx = (c * 150 + i * 80) % 1500 + 50
            cy = 200 + (c % 4) * 220
            draw.ellipse([cx, cy, cx + 180, cy + 180], fill=((c * 35) % 255, (c * 70) % 255, 240), outline=(255, 255, 255), width=4)
        draw.text((100, 100), f"HIGH RESOLUTION GRAPHIC ASSET {i+1} [DPI 300 - UNCOMPRESSED]", fill=(255, 255, 255))
        
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=98)
        images.append(buf.getvalue())

    # Page 1: Cover
    p1 = doc.new_page(width=595, height=842)
    p1.draw_rect(fitz.Rect(0, 0, 595, 842), color=None, fill=(0.05, 0.1, 0.2))
    p1.insert_image(fitz.Rect(40, 120, 555, 480), stream=images[0])
    p1.insert_text(fitz.Point(50, 540), "NEXUS HARDWARE SYSTEMS", fontsize=24, color=(1, 1, 1), fontname="helv")
    p1.insert_text(fitz.Point(50, 570), "2026 PRODUCT SPECIFICATION & CAMERA CATALOG", fontsize=13, color=(0.4, 0.8, 1), fontname="helv")
    p1.insert_text(fitz.Point(50, 610), "Full Resolution Media Edition • Intended for High-Impact Display & Print", fontsize=10, color=(0.8, 0.8, 0.8), fontname="helv")

    # Page 2: Product Sheet 1
    p2 = doc.new_page(width=595, height=842)
    p2.insert_text(fitz.Point(50, 60), "MODEL X-900 OPTICAL SENSOR", fontsize=18, color=(0.1, 0.2, 0.3), fontname="helv")
    p2.insert_text(fitz.Point(50, 80), "Ultra-wide dynamic spectrum with multi-layer coating.", fontsize=10, color=(0.5, 0.5, 0.5), fontname="helv")
    p2.insert_image(fitz.Rect(50, 100, 545, 420), stream=images[1])
    spec_text = (
        "Technical Highlights:\n"
        "• Resolution: 108 Megapixels Raw Uncompressed\n"
        "• Focal Length: 24mm - 70mm f/1.4 Constant Aperture\n"
        "• High Bitrate Processing: Dual Hardware Neural Coprocessors\n"
        "• Environmental Rating: IP68 Hermetically Sealed Ceramic Enclosure\n\n"
        "This sample document contains high-density raster imagery engineered to thoroughly exercise PDFchemy's "
        "Smart Compression presets (Extreme, Strong, Moderate, Light) and Grayscale Optimizer."
    )
    p2.insert_textbox(fitz.Rect(50, 450, 545, 750), spec_text, fontsize=10, fontname="helv", lineheight=1.5)

    # Page 3: Product Sheet 2
    p3 = doc.new_page(width=595, height=842)
    p3.insert_text(fitz.Point(50, 60), "INTELLIGENT EDGE CONTROLLER", fontsize=18, color=(0.1, 0.2, 0.3), fontname="helv")
    p3.insert_image(fitz.Rect(50, 100, 545, 420), stream=images[2])
    p3.insert_textbox(fitz.Rect(50, 450, 545, 750), 
                      "The Edge Controller offers dedicated real-time vector processing and hardware cryptographic acceleration.\n"
                      "Optimized for zero-cloud latency and complete on-premises data isolation.",
                      fontsize=10, fontname="helv", lineheight=1.5)

    path = os.path.join(OUTPUT_DIR, "03_Product_Catalog_Heavy_Graphic.pdf")
    doc.save(path)
    print(f"✓ Saved: {path} (Size: {os.path.getsize(path) / (1024*1024):.2f} MB)")

# -------------------------------------------------------------
# 4. MULTI-PAGE MANUAL WITH BOOKMARKS (FOR BOOKLET / N-UP / ORGANIZE)
# -------------------------------------------------------------
def generate_multipage_manual():
    doc = fitz.open()
    
    sections = [
        ("Executive Overview & Architecture", "Section 1", 
         "This architectural blueprint outlines the principles of local-first mobile software engineering. "
         "By confining all parsing, transformations, and indexing to the edge runtime, the application "
         "guarantees non-negotiable confidentiality, zero cloud latency, and resilience against network failures."),
        ("Security Protocols & Cryptographic Keying", "Section 2", 
         "Document security requires strict algorithmic isolation. Standard AES-256 with CBC/GCM modes and "
         "authenticated encryption ensures that files stored on flash storage remain immune to unauthorized inspection."),
        ("Memory Allocation & Native Pipeline Scaling", "Section 3", 
         "Managing multi-gigabyte PDF streams within tight Android heap constraints (e.g. 192MB - 512MB limits) "
         "demands careful tile rendering, lazy bitmap recycling, and memory-mapped file handles."),
        ("Typography, Fonts & Vector Layout Imposition", "Section 4", 
         "Vector rendering fidelity depends on accurate font descriptor matching and CMap glyph translation. "
         "Imposition engines (2-Up, 4-Up, and Saddle-Stitch Booklets) calculate affine transformation matrices "
         "to map source media boxes onto target printer sheets without rasterization artifacts."),
        ("Form Flattening & Annotations Preservation", "Section 5", 
         "AcroForm widgets often exhibit inconsistent visual states across diverse PDF viewers. By rasterizing "
         "or rendering appearance streams (/AP) directly into the page content stream (/Contents), signatures "
         "and checkboxes are locked permanently."),
        ("OCR Recognition & Spatial Layout Analysis", "Section 6", 
         "Optical Character Recognition requires pre-processing pipelines: binarization, skew correction, "
         "and contrast normalization. Text bounding boxes are mapped to an invisible text layer over the source scan."),
        ("Document Repair & Damaged Header Recovery", "Section 7", 
         "Truncated cross-reference tables (XREF) and corrupted trailer dictionaries represent 85% of PDF errors. "
         "The heuristic scanner reconstructs object offsets by traversing binary stream markers."),
        ("Appendix: Compliance, Standards & Citations", "Appendix", 
         "Conforms to ISO 32000-1, ISO 32000-2 (PDF 2.0), and PDF/A-1b archiving standards. All processing executed "
         "within Android sandbox with zero outbound network sockets.")
    ]
    
    toc = []
    
    for idx, (title, sec_num, content) in enumerate(sections):
        p = doc.new_page(width=595, height=842)
        page_num = idx + 1
        
        # Header
        p.draw_line(fitz.Point(50, 45), fitz.Point(545, 45), color=(0.7, 0.7, 0.7), width=0.5)
        p.insert_text(fitz.Point(50, 40), f"PDFCHEMY TECHNICAL MANUAL • {sec_num}", fontsize=7.5, color=(0.4, 0.4, 0.4), fontname="helv")
        p.insert_text(fitz.Point(500, 40), f"Page {page_num}", fontsize=7.5, color=(0.4, 0.4, 0.4), fontname="helv")
        
        # Section Title
        p.insert_text(fitz.Point(50, 100), title, fontsize=18, color=(0.12, 0.25, 0.4), fontname="helv")
        p.insert_text(fitz.Point(50, 120), f"Published: September 2026 • Reference Code: MAN-00{page_num}", fontsize=8.5, color=(0.5, 0.5, 0.5), fontname="helv")
        p.draw_line(fitz.Point(50, 135), fitz.Point(545, 135), color=(0.85, 0.85, 0.85), width=1)
        
        # Body text duplicated to fill page nicely
        full_text = content + "\n\n" + (content + "\n\n") * 3
        p.insert_textbox(fitz.Rect(50, 160, 545, 750), full_text, fontsize=10.5, fontname="times-roman", lineheight=1.45)
        
        # Footer
        p.draw_line(fitz.Point(50, 780), fitz.Point(545, 780), color=(0.8, 0.8, 0.8), width=0.5)
        p.insert_text(fitz.Point(50, 795), "Internal Technical Reference • Confidential & Proprietary", fontsize=7.5, color=(0.5, 0.5, 0.5), fontname="helv")
        p.insert_text(fitz.Point(480, 795), f"Sheet {page_num} of {len(sections)}", fontsize=7.5, color=(0.5, 0.5, 0.5), fontname="helv")
        
        # Outline bookmark
        toc.append([1, title, page_num])
        
    doc.set_toc(toc)
    path = os.path.join(OUTPUT_DIR, "04_Research_Paper_MultiPage_Manual.pdf")
    doc.save(path)
    print(f"✓ Saved: {path} (8 pages with TOC bookmarks)")

# -------------------------------------------------------------
# 5. REALISTIC SCANNED EXPENSE RECEIPT (FOR OCR & SCAN CLEANUP)
# -------------------------------------------------------------
def generate_scanned_receipt():
    from PIL import Image, ImageDraw, ImageFont, ImageFilter
    import random
    
    # Create realistic scanned receipt bitmap with texture and slight rotation
    w, h = 1000, 1500
    img = Image.new("L", (w, h), color=245)
    draw = ImageDraw.Draw(img)
    
    # Paper noise/grain
    random.seed(42)
    pixels = img.load()
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            noise = random.randint(-12, 12)
            val = max(180, min(255, 240 + noise))
            pixels[x, y] = val
            if x + 1 < w: pixels[x + 1, y] = val
            if y + 1 < h: pixels[x, y + 1] = val

    # Draw receipt store header
    draw.text((280, 80), "METROPOLIS BISTRO & CAFE", fill=30)
    draw.text((320, 115), "104 Main Street, Financial District", fill=60)
    draw.text((350, 140), "Tel: +1 (555) 019-2834", fill=60)
    draw.text((310, 165), "Tax ID / VAT: US-99201928-01", fill=60)
    
    # Separator dashed
    draw.text((100, 200), "--------------------------------------------------------------------------------", fill=60)
    draw.text((120, 230), "DATE: 09/03/2026   12:45 PM", fill=30)
    draw.text((650, 230), "TABLE: 14   SRV: ALEX", fill=30)
    draw.text((100, 260), "--------------------------------------------------------------------------------", fill=60)
    
    # Items
    receipt_items = [
        ("1  ESPRESSO DOUBLE SHOT", "$5.50"),
        ("2  AVOCADO TOAST DELUXE", "$28.00"),
        ("1  SPARKLING MINERAL WATER", "$4.75"),
        ("1  GRILLED SALMON SALAD", "$22.50"),
        ("2  CROISSANT ALMOND", "$9.00"),
        ("1  ORGANIC GREEN TEA", "$4.25")
    ]
    
    y = 300
    for name, price in receipt_items:
        draw.text((120, y), name, fill=20)
        draw.text((750, y), price, fill=20)
        y += 45
        
    draw.text((100, y + 10), "--------------------------------------------------------------------------------", fill=60)
    y += 40
    draw.text((120, y), "SUBTOTAL:", fill=30)
    draw.text((750, y), "$74.00", fill=30)
    y += 35
    draw.text((120, y), "CITY TAX (8.875%):", fill=60)
    draw.text((750, y), "$6.57", fill=60)
    y += 35
    draw.text((120, y), "TIP (18%):", fill=60)
    draw.text((750, y), "$13.32", fill=60)
    y += 40
    draw.text((100, y), "================================================================================", fill=20)
    y += 30
    draw.text((120, y), "TOTAL AMOUNT PAID:", fill=10)
    draw.text((730, y), "$93.89", fill=10)
    y += 40
    draw.text((100, y), "================================================================================", fill=20)
    y += 50
    draw.text((220, y), "PAYMENT METHOD: VISA ENDING IN *4912", fill=40)
    draw.text((250, y + 30), "AUTH CODE: #994812 - APPROVED", fill=40)
    draw.text((280, y + 70), "THANK YOU FOR YOUR PATRONAGE!", fill=40)
    draw.text((320, y + 95), "SAVE THIS RECEIPT FOR EXPENSES", fill=60)
    
    # Slight scan rotation & blur to mimic realistic phone scan
    img = img.rotate(0.8, fillcolor=255, expand=False)
    img = img.filter(ImageFilter.GaussianBlur(0.4))
    
    import io
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=85)
    
    doc = fitz.open()
    page = doc.new_page(width=595, height=842)
    # Center receipt on page
    page.insert_image(fitz.Rect(50, 40, 545, 780), stream=buf.getvalue())
    
    path = os.path.join(OUTPUT_DIR, "05_Scanned_Expense_Receipt_Dirty.pdf")
    doc.save(path)
    print(f"✓ Saved: {path}")

# Run all
generate_contract()
generate_invoice()
generate_product_catalog()
generate_multipage_manual()
generate_scanned_receipt()

print("\nAll 5 PDF mockups generated successfully!")
