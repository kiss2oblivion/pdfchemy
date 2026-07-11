import os
import re

translations = {
    "msg_enter_text": {
        "en": "Please enter some text.", "de": "Bitte geben Sie Text ein.", "es": "Por favor, introduce texto.",
        "fr": "Veuillez entrer du texte.", "in": "Silakan masukkan teks.", "it": "Inserisci del testo.",
        "pt": "Por favor, introduza algum texto.", "pt-rBR": "Por favor, insira algum texto.", "ro": "Te rugăm să introduci text."
    },
    "menu_delete_pages": {
        "en": "Delete Pages", "de": "Seiten löschen", "es": "Eliminar páginas",
        "fr": "Supprimer des pages", "in": "Hapus Halaman", "it": "Elimina pagine",
        "pt": "Eliminar Páginas", "pt-rBR": "Excluir Páginas", "ro": "Șterge Pagini"
    },
    "menu_delete_pages_desc": {
        "en": "Remove specific pages from a PDF", "de": "Bestimmte Seiten aus PDF entfernen", "es": "Elimina páginas específicas de un PDF",
        "fr": "Supprimer des pages spécifiques", "in": "Hapus halaman tertentu dari PDF", "it": "Rimuovi pagine specifiche da un PDF",
        "pt": "Remover páginas específicas de um PDF", "pt-rBR": "Remover páginas específicas de um PDF", "ro": "Elimină pagini specifice dintr-un PDF"
    },
    "menu_extract_images": {
        "en": "Extract Images", "de": "Bilder extrahieren", "es": "Extraer imágenes",
        "fr": "Extraire des images", "in": "Ekstrak Gambar", "it": "Estrai immagini",
        "pt": "Extrair Imagens", "pt-rBR": "Extrair Imagens", "ro": "Extrage Imagini"
    },
    "menu_extract_images_desc": {
        "en": "Extract all images embedded in a PDF", "de": "Alle eingebetteten Bilder extrahieren", "es": "Extrae todas las imágenes de un PDF",
        "fr": "Extraire toutes les images intégrées", "in": "Ekstrak semua gambar dalam PDF", "it": "Estrai tutte le immagini incorporate",
        "pt": "Extrair todas as imagens incorporadas num PDF", "pt-rBR": "Extrair todas as imagens embutidas em um PDF", "ro": "Extrage toate imaginile din PDF"
    },
    "menu_rotate_pages": {
        "en": "Rotate Pages", "de": "Seiten drehen", "es": "Rotar páginas",
        "fr": "Faire pivoter les pages", "in": "Putar Halaman", "it": "Ruota pagine",
        "pt": "Rodar Páginas", "pt-rBR": "Rotacionar Páginas", "ro": "Rotește Pagini"
    },
    "menu_rotate_pages_desc": {
        "en": "Rotate specific pages or entire documents", "de": "Bestimmte Seiten oder ganzes Dokument drehen", "es": "Rota páginas específicas o el documento",
        "fr": "Faire pivoter des pages spécifiques", "in": "Putar halaman tertentu atau seluruh dokumen", "it": "Ruota pagine specifiche o l'intero documento",
        "pt": "Rodar páginas específicas ou documentos inteiros", "pt-rBR": "Rotacionar páginas específicas ou documentos inteiros", "ro": "Rotește pagini specifice sau documente întregi"
    },
    "msg_extracted_images": {
        "en": "Extracted %1$d images successfully!", "de": "%1$d Bilder erfolgreich extrahiert!", "es": "¡Se extrajeron %1$d imágenes!",
        "fr": "%1$d images extraites avec succès !", "in": "Berhasil mengekstrak %1$d gambar!", "it": "%1$d immagini estratte con successo!",
        "pt": "Extraídas %1$d imagens com sucesso!", "pt-rBR": "Extraídas %1$d imagens com sucesso!", "ro": "S-au extras %1$d imagini!"
    },
    "msg_failed_extract_images": {
        "en": "Failed to extract images (%1$d errors).", "de": "Fehler beim Extrahieren (%1$d Fehler).", "es": "Error al extraer imágenes (%1$d errores).",
        "fr": "Échec de l'extraction (%1$d erreurs).", "in": "Gagal mengekstrak gambar (%1$d kesalahan).", "it": "Impossibile estrarre immagini (%1$d errori).",
        "pt": "Falha ao extrair imagens (%1$d erros).", "pt-rBR": "Falha ao extrair imagens (%1$d erros).", "ro": "Eroare la extragere (%1$d erori)."
    },
    "msg_no_images_found": {
        "en": "No images found in this PDF.", "de": "Keine Bilder in dieser PDF gefunden.", "es": "No se encontraron imágenes en este PDF.",
        "fr": "Aucune image trouvée.", "in": "Tidak ada gambar ditemukan di PDF ini.", "it": "Nessuna immagine trovata nel PDF.",
        "pt": "Nenhuma imagem encontrada neste PDF.", "pt-rBR": "Nenhuma imagem encontrada neste PDF.", "ro": "Nicio imagine găsită în acest PDF."
    },
    "menu_inspect_metadata": {
        "en": "Inspect &amp; Edit Metadata", "de": "Metadaten prüfen &amp; bearbeiten", "es": "Inspeccionar y editar metadatos",
        "fr": "Inspecter et éditer les métadonnées", "in": "Periksa &amp; Edit Metadata", "it": "Ispeziona e modifica metadati",
        "pt": "Inspecionar e Editar Metadados", "pt-rBR": "Inspecionar e Editar Metadados", "ro": "Inspectează și editează metadate"
    },
    "menu_inspect_metadata_desc": {
        "en": "View document stats and modify metadata fields", "de": "Dokumentstatistiken anzeigen und Metadaten ändern", "es": "Ver estadísticas y modificar campos de metadatos",
        "fr": "Afficher les stats et modifier les métadonnées", "in": "Lihat statistik dokumen dan ubah metadata", "it": "Visualizza statistiche e modifica metadati",
        "pt": "Ver estatísticas do documento e modificar metadados", "pt-rBR": "Ver estatísticas do documento e modificar metadados", "ro": "Vezi statistici și modifică metadatele"
    },
    "menu_remove_metadata": {
        "en": "Remove Metadata", "de": "Metadaten entfernen", "es": "Eliminar metadatos",
        "fr": "Supprimer les métadonnées", "in": "Hapus Metadata", "it": "Rimuovi metadati",
        "pt": "Remover Metadados", "pt-rBR": "Remover Metadados", "ro": "Elimină Metadate"
    },
    "menu_remove_metadata_desc": {
        "en": "Strip all metadata properties from the PDF", "de": "Alle Metadateneigenschaften aus der PDF entfernen", "es": "Elimina todas las propiedades de metadatos del PDF",
        "fr": "Supprimer toutes les propriétés de métadonnées", "in": "Hapus semua properti metadata dari PDF", "it": "Rimuovi tutte le proprietà dei metadati",
        "pt": "Remover todas as propriedades de metadados do PDF", "pt-rBR": "Remover todas as propriedades de metadados do PDF", "ro": "Elimină toate metadatele din PDF"
    },
    "menu_text_cleaner": {
        "en": "Text Cleaner", "de": "Text-Reiniger", "es": "Limpiador de texto",
        "fr": "Nettoyeur de texte", "in": "Pembersih Teks", "it": "Pulitore di testo",
        "pt": "Limpador de Texto", "pt-rBR": "Limpador de Texto", "ro": "Curățător Text"
    },
    "menu_text_cleaner_desc": {
        "en": "Clean, format, and organize text directly", "de": "Text direkt bereinigen, formatieren und organisieren", "es": "Limpia, formatea y organiza el texto directamente",
        "fr": "Nettoyer, formater et organiser le texte", "in": "Bersihkan, format, dan atur teks", "it": "Pulisci, formatta e organizza il testo",
        "pt": "Limpar, formatar e organizar texto", "pt-rBR": "Limpar, formatar e organizar texto", "ro": "Curăță, formatează și organizează text"
    },
    "msg_scanned_pdf_exported": {
        "en": "Scanned PDF exported!", "de": "Gescannte PDF exportiert!", "es": "¡PDF escaneado exportado!",
        "fr": "PDF numérisé exporté !", "in": "PDF yang dipindai berhasil diekspor!", "it": "PDF scansionato esportato!",
        "pt": "PDF digitalizado exportado!", "pt-rBR": "PDF escaneado exportado!", "ro": "PDF scanat exportat!"
    },
    "msg_scanner_not_available": {
        "en": "Scanner not available.", "de": "Scanner nicht verfügbar.", "es": "Escáner no disponible.",
        "fr": "Scanner non disponible.", "in": "Pemindai tidak tersedia.", "it": "Scanner non disponibile.",
        "pt": "Scanner não disponível.", "pt-rBR": "Scanner não disponível.", "ro": "Scanner indisponibil."
    },
    "msg_history_cleared": {
        "en": "History cleared", "de": "Verlauf gelöscht", "es": "Historial borrado",
        "fr": "Historique effacé", "in": "Riwayat dibersihkan", "it": "Cronologia cancellata",
        "pt": "Histórico limpo", "pt-rBR": "Histórico limpo", "ro": "Istoric șters"
    },
    "label_type_or_paste": {
        "en": "Type or paste %1$s here", "de": "Tippen oder fügen Sie %1$s hier ein", "es": "Escribe o pega %1$s aquí",
        "fr": "Tapez ou collez %1$s ici", "in": "Ketik atau tempel %1$s di sini", "it": "Digita o incolla %1$s qui",
        "pt": "Digite ou cole %1$s aqui", "pt-rBR": "Digite ou cole %1$s aqui", "ro": "Tastează sau lipește %1$s aici"
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

# Fix TextConverterScreen.kt
tc_path = "app/src/main/java/com/example/shrinkpdf/ui/textconverter/TextConverterScreen.kt"
with open(tc_path, 'r', encoding='utf-8') as f:
    tc_code = f.read()
tc_code = tc_code.replace('label = { Text("Type or paste ${inputFormat.name} here") },',
                          'label = { Text(stringResource(R.string.label_type_or_paste, inputFormat.name)) },')
tc_code = tc_code.replace('Toast.makeText(context, "Please enter some text.", Toast.LENGTH_SHORT).show()',
                          'Toast.makeText(context, context.getString(R.string.msg_enter_text), Toast.LENGTH_SHORT).show()')
with open(tc_path, 'w', encoding='utf-8') as f:
    f.write(tc_code)

# Fix OrganizeScreens.kt
org_path = "app/src/main/java/com/example/shrinkpdf/ui/OrganizeScreens.kt"
with open(org_path, 'r', encoding='utf-8') as f:
    org_code = f.read()
org_code = org_code.replace('title = "Merge PDFs",', 'title = stringResource(R.string.menu_merge),')
org_code = org_code.replace('subtitle = "Combine multiple PDFs into a single document",', 'subtitle = stringResource(R.string.menu_merge_desc),')
org_code = org_code.replace('title = "Split PDF",', 'title = stringResource(R.string.menu_split),')
org_code = org_code.replace('subtitle = "Extract pages or separate a PDF into multiple files",', 'subtitle = stringResource(R.string.menu_split_desc),')
org_code = org_code.replace('title = "Delete Pages",', 'title = stringResource(R.string.menu_delete_pages),')
org_code = org_code.replace('subtitle = "Remove specific pages from a PDF",', 'subtitle = stringResource(R.string.menu_delete_pages_desc),')
org_code = org_code.replace('title = "Extract Images",', 'title = stringResource(R.string.menu_extract_images),')
org_code = org_code.replace('subtitle = "Extract all images embedded in a PDF",', 'subtitle = stringResource(R.string.menu_extract_images_desc),')
org_code = org_code.replace('title = "Rotate Pages",', 'title = stringResource(R.string.menu_rotate_pages),')
org_code = org_code.replace('subtitle = "Rotate specific pages or entire documents",', 'subtitle = stringResource(R.string.menu_rotate_pages_desc),')

org_code = org_code.replace('Toast.makeText(context, "Extracted $extracted images successfully!", Toast.LENGTH_LONG).show()',
                            'Toast.makeText(context, context.getString(R.string.msg_extracted_images, extracted), Toast.LENGTH_LONG).show()')
org_code = org_code.replace('Toast.makeText(context, "Failed to extract images ($errors errors).", Toast.LENGTH_SHORT).show()',
                            'Toast.makeText(context, context.getString(R.string.msg_failed_extract_images, errors), Toast.LENGTH_SHORT).show()')
org_code = org_code.replace('Toast.makeText(context, "No images found in this PDF.", Toast.LENGTH_SHORT).show()',
                            'Toast.makeText(context, context.getString(R.string.msg_no_images_found), Toast.LENGTH_SHORT).show()')
with open(org_path, 'w', encoding='utf-8') as f:
    f.write(org_code)

# Fix CheckScreens.kt
check_path = "app/src/main/java/com/example/shrinkpdf/ui/CheckScreens.kt"
with open(check_path, 'r', encoding='utf-8') as f:
    check_code = f.read()
check_code = check_code.replace('title = "Inspect & Edit Metadata",', 'title = stringResource(R.string.menu_inspect_metadata),')
check_code = check_code.replace('subtitle = "View document stats and modify metadata fields",', 'subtitle = stringResource(R.string.menu_inspect_metadata_desc),')
check_code = check_code.replace('title = "Remove Metadata",', 'title = stringResource(R.string.menu_remove_metadata),')
check_code = check_code.replace('subtitle = "Strip all metadata properties from the PDF",', 'subtitle = stringResource(R.string.menu_remove_metadata_desc),')
check_code = check_code.replace('title = "Text Cleaner",', 'title = stringResource(R.string.menu_text_cleaner),')
check_code = check_code.replace('subtitle = "Clean, format, and organize text directly",', 'subtitle = stringResource(R.string.menu_text_cleaner_desc),')
check_code = check_code.replace('title = "Extract Text / OCR",', 'title = stringResource(R.string.menu_extract),')
check_code = check_code.replace('subtitle = "Extract text from PDFs or scanned documents",', 'subtitle = stringResource(R.string.menu_extract_desc),')
with open(check_path, 'w', encoding='utf-8') as f:
    f.write(check_code)

# Fix MainActivity.kt
ma_path = "app/src/main/java/com/example/shrinkpdf/MainActivity.kt"
with open(ma_path, 'r', encoding='utf-8') as f:
    ma_code = f.read()
ma_code = ma_code.replace('Toast.makeText(context, "Scanned PDF exported!", Toast.LENGTH_SHORT).show()',
                          'Toast.makeText(context, context.getString(R.string.msg_scanned_pdf_exported), Toast.LENGTH_SHORT).show()')
ma_code = ma_code.replace('Toast.makeText(context, "Scanner not available.", Toast.LENGTH_SHORT).show()',
                          'Toast.makeText(context, context.getString(R.string.msg_scanner_not_available), Toast.LENGTH_SHORT).show()')
ma_code = ma_code.replace('Toast.makeText(context, "Please enter some text.", Toast.LENGTH_SHORT).show()',
                          'Toast.makeText(context, context.getString(R.string.msg_enter_text), Toast.LENGTH_SHORT).show()')
ma_code = ma_code.replace('Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()',
                          'Toast.makeText(context, context.getString(R.string.msg_history_cleared), Toast.LENGTH_SHORT).show()')
with open(ma_path, 'w', encoding='utf-8') as f:
    f.write(ma_code)

print("Patch 2 applied.")
