import os

translations = {
    "label_words": {
        "en": "Words: %1$d", "de": "Wörter: %1$d", "es": "Palabras: %1$d",
        "fr": "Mots : %1$d", "in": "Kata: %1$d", "it": "Parole: %1$d",
        "pt": "Palavras: %1$d", "pt-rBR": "Palavras: %1$d", "ro": "Cuvinte: %1$d"
    },
    "label_chars": {
        "en": "Characters: %1$d", "de": "Zeichen: %1$d", "es": "Caracteres: %1$d",
        "fr": "Caractères : %1$d", "in": "Karakter: %1$d", "it": "Caratteri: %1$d",
        "pt": "Caracteres: %1$d", "pt-rBR": "Caracteres: %1$d", "ro": "Caractere: %1$d"
    },
    "label_from_format": {
        "en": "From: %1$s", "de": "Von: %1$s", "es": "De: %1$s",
        "fr": "De : %1$s", "in": "Dari: %1$s", "it": "Da: %1$s",
        "pt": "De: %1$s", "pt-rBR": "De: %1$s", "ro": "De la: %1$s"
    },
    "label_to_format": {
        "en": "To: %1$s", "de": "Zu: %1$s", "es": "A: %1$s",
        "fr": "À : %1$s", "in": "Ke: %1$s", "it": "A: %1$s",
        "pt": "Para: %1$s", "pt-rBR": "Para: %1$s", "ro": "Către: %1$s"
    },
    "action_save_as": {
        "en": "Save as %1$s", "de": "Speichern als %1$s", "es": "Guardar como %1$s",
        "fr": "Enregistrer sous %1$s", "in": "Simpan sebagai %1$s", "it": "Salva come %1$s",
        "pt": "Guardar como %1$s", "pt-rBR": "Salvar como %1$s", "ro": "Salvează ca %1$s"
    },
    "default_doc_name_pdf": {
        "en": "Document.pdf", "de": "Dokument.pdf", "es": "Documento.pdf",
        "fr": "Document.pdf", "in": "Dokumen.pdf", "it": "Documento.pdf",
        "pt": "Documento.pdf", "pt-rBR": "Documento.pdf", "ro": "Document.pdf"
    },
    "default_doc_name": {
        "en": "Document", "de": "Dokument", "es": "Documento",
        "fr": "Document", "in": "Dokumen", "it": "Documento",
        "pt": "Documento", "pt-rBR": "Documento", "ro": "Document"
    },
    "label_pages_count": {
        "en": "Pages: %1$d", "de": "Seiten: %1$d", "es": "Páginas: %1$d",
        "fr": "Pages : %1$d", "in": "Halaman: %1$d", "it": "Pagine: %1$d",
        "pt": "Páginas: %1$d", "pt-rBR": "Páginas: %1$d", "ro": "Pagini: %1$d"
    },
    "label_images_count": {
        "en": "Images: %1$d", "de": "Bilder: %1$d", "es": "Imágenes: %1$d",
        "fr": "Images : %1$d", "in": "Gambar: %1$d", "it": "Immagini: %1$d",
        "pt": "Imagens: %1$d", "pt-rBR": "Imagens: %1$d", "ro": "Imagini: %1$d"
    },
    "label_signatures": {
        "en": "Signatures: %1$s", "de": "Signaturen: %1$s", "es": "Firmas: %1$s",
        "fr": "Signatures : %1$s", "in": "Tanda tangan: %1$s", "it": "Firme: %1$s",
        "pt": "Assinaturas: %1$s", "pt-rBR": "Assinaturas: %1$s", "ro": "Semnături: %1$s"
    },
    "label_type": {
        "en": "Type: %1$s", "de": "Typ: %1$s", "es": "Tipo: %1$s",
        "fr": "Type : %1$s", "in": "Tipe: %1$s", "it": "Tipo: %1$s",
        "pt": "Tipo: %1$s", "pt-rBR": "Tipo: %1$s", "ro": "Tip: %1$s"
    },
    "label_total_summary": {
        "en": "Total Summary (%1$d files)", "de": "Gesamtzusammenfassung (%1$d Dateien)", "es": "Resumen total (%1$d archivos)",
        "fr": "Résumé total (%1$d fichiers)", "in": "Ringkasan Total (%1$d file)", "it": "Riepilogo totale (%1$d file)",
        "pt": "Resumo Total (%1$d ficheiros)", "pt-rBR": "Resumo Total (%1$d arquivos)", "ro": "Rezumat Total (%1$d fișiere)"
    },
    "action_hide_files": {
        "en": "Hide Individual Files", "de": "Einzelne Dateien ausblenden", "es": "Ocultar archivos individuales",
        "fr": "Masquer les fichiers", "in": "Sembunyikan File Individu", "it": "Nascondi file individuali",
        "pt": "Ocultar Ficheiros", "pt-rBR": "Ocultar Arquivos", "ro": "Ascunde Fișierele"
    },
    "action_view_files": {
        "en": "View Individual Files", "de": "Einzelne Dateien anzeigen", "es": "Ver archivos individuales",
        "fr": "Voir les fichiers", "in": "Lihat File Individu", "it": "Visualizza file individuali",
        "pt": "Ver Ficheiros", "pt-rBR": "Ver Arquivos", "ro": "Vezi Fișierele"
    },
    "value_yes": {
        "en": "Yes", "de": "Ja", "es": "Sí",
        "fr": "Oui", "in": "Ya", "it": "Sì",
        "pt": "Sim", "pt-rBR": "Sim", "ro": "Da"
    },
    "value_no": {
        "en": "No", "de": "Nein", "es": "No",
        "fr": "Non", "in": "Tidak", "it": "No",
        "pt": "Não", "pt-rBR": "Não", "ro": "Nu"
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

# Fix TextScreens.kt
ts_path = "app/src/main/java/com/example/shrinkpdf/ui/TextScreens.kt"
with open(ts_path, 'r', encoding='utf-8') as f:
    ts_code = f.read()
ts_code = ts_code.replace('Text("Words: $wordCount",', 'Text(stringResource(R.string.label_words, wordCount),')
ts_code = ts_code.replace('Text("Characters: $charCount",', 'Text(stringResource(R.string.label_chars, charCount),')
with open(ts_path, 'w', encoding='utf-8') as f:
    f.write(ts_code)

# Fix TextConverterScreen.kt
tc_path = "app/src/main/java/com/example/shrinkpdf/ui/textconverter/TextConverterScreen.kt"
with open(tc_path, 'r', encoding='utf-8') as f:
    tc_code = f.read()
tc_code = tc_code.replace('Text("From: ${inputFormat.name}")', 'Text(stringResource(R.string.label_from_format, inputFormat.name))')
tc_code = tc_code.replace('Text("To: ${outputFormat.name}")', 'Text(stringResource(R.string.label_to_format, outputFormat.name))')
tc_code = tc_code.replace('Text("Save as ${outputFormat.name}")', 'Text(stringResource(R.string.action_save_as, outputFormat.name))')
with open(tc_path, 'w', encoding='utf-8') as f:
    f.write(tc_code)

# Fix OrganizeScreens.kt
org_path = "app/src/main/java/com/example/shrinkpdf/ui/OrganizeScreens.kt"
with open(org_path, 'r', encoding='utf-8') as f:
    org_code = f.read()
org_code = org_code.replace('Text(selectedPdfName ?: "Document.pdf",', 'Text(selectedPdfName ?: stringResource(R.string.default_doc_name_pdf),')
org_code = org_code.replace('Text(file.name ?: "Document",', 'Text(file.name ?: stringResource(R.string.default_doc_name),')
with open(org_path, 'w', encoding='utf-8') as f:
    f.write(org_code)

# Fix CheckScreens.kt
check_path = "app/src/main/java/com/example/shrinkpdf/ui/CheckScreens.kt"
with open(check_path, 'r', encoding='utf-8') as f:
    check_code = f.read()
check_code = check_code.replace('Text("Pages: ${a.pageCount}")', 'Text(stringResource(R.string.label_pages_count, a.pageCount))')
check_code = check_code.replace('Text("Images: ${a.imageCount}")', 'Text(stringResource(R.string.label_images_count, a.imageCount))')
check_code = check_code.replace('Text("Signatures: ${if (a.hasSignatures) "Yes" else "No"}")', 'Text(stringResource(R.string.label_signatures, if (a.hasSignatures) stringResource(R.string.value_yes) else stringResource(R.string.value_no)))')
check_code = check_code.replace('Text("Type: ${a.scenario.name}")', 'Text(stringResource(R.string.label_type, a.scenario.name))')
check_code = check_code.replace('Text(file.name ?: "Document",', 'Text(file.name ?: stringResource(R.string.default_doc_name),')
with open(check_path, 'w', encoding='utf-8') as f:
    f.write(check_code)

# Fix MainActivity.kt
ma_path = "app/src/main/java/com/example/shrinkpdf/MainActivity.kt"
with open(ma_path, 'r', encoding='utf-8') as f:
    ma_code = f.read()
ma_code = ma_code.replace('Text("Total Summary (${selectedFiles.size} files)",', 'Text(stringResource(R.string.label_total_summary, selectedFiles.size),')
ma_code = ma_code.replace('Text(if (isListExpanded) "Hide Individual Files" else "View Individual Files")', 'Text(if (isListExpanded) stringResource(R.string.action_hide_files) else stringResource(R.string.action_view_files))')
with open(ma_path, 'w', encoding='utf-8') as f:
    f.write(ma_code)

print("Patch 4 applied.")
