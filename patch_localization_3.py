import os

translations = {
    "menu_extract": {
        "en": "Extract Text / OCR", "de": "Text extrahieren / OCR", "es": "Extraer texto / OCR",
        "fr": "Extraire le texte / OCR", "in": "Ekstrak Teks / OCR", "it": "Estrai testo / OCR",
        "pt": "Extrair Texto / OCR", "pt-rBR": "Extrair Texto / OCR", "ro": "Extrage Text / OCR"
    },
    "menu_extract_desc": {
        "en": "Extract text from PDFs or scanned documents", "de": "Text aus PDFs oder Scans extrahieren", "es": "Extrae texto de PDFs o escaneos",
        "fr": "Extraire le texte de PDF ou de scans", "in": "Ekstrak teks dari PDF atau dokumen yang dipindai", "it": "Estrai testo da PDF o documenti scansionati",
        "pt": "Extrair texto de PDFs ou documentos digitalizados", "pt-rBR": "Extrair texto de PDFs ou documentos escaneados", "ro": "Extrage text din PDF-uri sau documente scanate"
    }
}

base_dir = "app/src/main/res"
lang_map = {
    "en": "values",
    "de": "values-de",
    "es": "values-es",
    "fr": "values-fr",
    "in": "values-in",
    "it": "values-it",
    "pt": "values-pt",
    "pt-rBR": "values-pt-rBR",
    "ro": "values-ro"
}

for lang_code, folder in lang_map.items():
    file_path = os.path.join(base_dir, folder, "strings.xml")
    if not os.path.exists(file_path):
        continue
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    insert_str = ""
    for key, trans_dict in translations.items():
        if f'name="{key}"' in content:
            continue
        val = trans_dict.get(lang_code, trans_dict["en"])
        val = val.replace("'", "\\'").replace('"', '\\"')
        insert_str += f'    <string name="{key}">{val}</string>\n'
    
    new_content = content.replace("</resources>", insert_str + "</resources>")
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

print("Strings added to all XMLs")
