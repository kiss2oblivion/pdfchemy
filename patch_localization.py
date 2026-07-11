import os
import re

translations = {
    "msg_failed_export": {
        "en": "Failed to export PDF", "de": "PDF-Export fehlgeschlagen", "es": "Error al exportar PDF", 
        "fr": "Échec de l'exportation du PDF", "in": "Gagal mengekspor PDF", "it": "Impossibile esportare il PDF",
        "pt": "Falha ao exportar PDF", "pt-rBR": "Falha ao exportar PDF", "ro": "Eroare la exportul PDF"
    },
    "msg_scan_saved": {
        "en": "Scan saved to Repository", "de": "Scan im Repository gespeichert", "es": "Escaneo guardado en el repositorio", 
        "fr": "Numérisation enregistrée", "in": "Pindaian disimpan", "it": "Scansione salvata",
        "pt": "Digitalização guardada", "pt-rBR": "Digitalização salva", "ro": "Scanare salvată"
    },
    "action_export": {
        "en": "Export", "de": "Exportieren", "es": "Exportar", 
        "fr": "Exporter", "in": "Ekspor", "it": "Esporta",
        "pt": "Exportar", "pt-rBR": "Exportar", "ro": "Exportă"
    },
    "msg_failed_scan": {
        "en": "Failed to process scan", "de": "Scan-Verarbeitung fehlgeschlagen", "es": "Error al procesar el escaneo", 
        "fr": "Échec du traitement", "in": "Gagal memproses pindaian", "it": "Impossibile elaborare",
        "pt": "Falha ao processar a digitalização", "pt-rBR": "Falha ao processar o escaneamento", "ro": "Eroare la procesarea scanării"
    },
    "subtitle_images_to_pdf": {
        "en": "Convert JPG, PNG, and other images to a single PDF", 
        "de": "JPG, PNG und andere Bilder in eine PDF konvertieren", 
        "es": "Convierte imágenes a un solo PDF", 
        "fr": "Convertir des images en un seul PDF", 
        "in": "Ubah gambar menjadi satu PDF", 
        "it": "Converti immagini in un singolo PDF",
        "pt": "Converter imagens num único PDF", 
        "pt-rBR": "Converter imagens em um único PDF", 
        "ro": "Convertește imagini într-un singur PDF"
    },
    "subtitle_text_to_pdf": {
        "en": "Convert your notes or .txt files to PDF", 
        "de": "Notizen oder .txt-Dateien in PDF konvertieren", 
        "es": "Convierte notas o archivos .txt a PDF", 
        "fr": "Convertissez vos notes en PDF", 
        "in": "Ubah catatan atau file .txt ke PDF", 
        "it": "Converti appunti o file .txt in PDF",
        "pt": "Converter notas ou ficheiros .txt para PDF", 
        "pt-rBR": "Converter notas ou arquivos .txt para PDF", 
        "ro": "Convertește notițe sau fișiere .txt în PDF"
    },
    "subtitle_format_converter": {
        "en": "Convert between TXT, MD, CSV, JSON, and more", 
        "de": "Konvertieren zwischen TXT, MD, CSV, JSON und mehr", 
        "es": "Convierte entre TXT, MD, CSV, JSON y más", 
        "fr": "Convertir entre TXT, MD, CSV, JSON et plus", 
        "in": "Konversi antara TXT, MD, CSV, JSON, dll.", 
        "it": "Converti tra TXT, MD, CSV, JSON e altri",
        "pt": "Converter entre TXT, MD, CSV, JSON e mais", 
        "pt-rBR": "Converter entre TXT, MD, CSV, JSON e mais", 
        "ro": "Conversie între TXT, MD, CSV, JSON și altele"
    },
    "subtitle_batch_compress": {
        "en": "Select a folder and compress all PDFs inside", 
        "de": "Ordner auswählen und alle PDFs komprimieren", 
        "es": "Selecciona una carpeta y comprime todos sus PDFs", 
        "fr": "Sélectionnez un dossier pour compresser tous les PDF", 
        "in": "Pilih folder dan kompres semua PDF di dalamnya", 
        "it": "Seleziona una cartella e comprimi tutti i PDF",
        "pt": "Selecionar uma pasta e comprimir todos os PDFs nela contidos", 
        "pt-rBR": "Selecione uma pasta e comprima todos os PDFs dentro", 
        "ro": "Selectează un dosar și comprimă toate PDF-urile"
    },
    "title_tools": {
        "en": "PDFchemy Tools", "de": "PDFchemy Werkzeuge", "es": "Herramientas de PDFchemy", 
        "fr": "Outils PDFchemy", "in": "Alat PDFchemy", "it": "Strumenti PDFchemy",
        "pt": "Ferramentas PDFchemy", "pt-rBR": "Ferramentas PDFchemy", "ro": "Unelte PDFchemy"
    },
    "file_name_label": {
        "en": "File Name: %1$s", "de": "Dateiname: %1$s", "es": "Nombre de Archivo: %1$s", 
        "fr": "Nom du fichier : %1$s", "in": "Nama File: %1$s", "it": "Nome file: %1$s",
        "pt": "Nome do Ficheiro: %1$s", "pt-rBR": "Nome do Arquivo: %1$s", "ro": "Nume Fișier: %1$s"
    },
    "selected_batch_files": {
        "en": "Selected Batch Files:", "de": "Ausgewählte Batch-Dateien:", "es": "Archivos Seleccionados:", 
        "fr": "Fichiers sélectionnés :", "in": "File Batch Terpilih:", "it": "File selezionati:",
        "pt": "Ficheiros Selecionados:", "pt-rBR": "Arquivos Selecionados:", "ro": "Fișiere Selectate:"
    },
    "analyzing_doc": {
        "en": "Analyzing document structure...", "de": "Dokumentenstruktur analysieren...", "es": "Analizando estructura...", 
        "fr": "Analyse du document...", "in": "Menganalisis dokumen...", "it": "Analisi della struttura...",
        "pt": "A analisar a estrutura do documento...", "pt-rBR": "Analisando estrutura do documento...", "ro": "Se analizează structura..."
    },
    "smart_recommendation": {
        "en": "Smart Recommendation", "de": "Intelligente Empfehlung", "es": "Recomendación Inteligente", 
        "fr": "Recommandation intelligente", "in": "Rekomendasi Cerdas", "it": "Raccomandazione intelligente",
        "pt": "Recomendação Inteligente", "pt-rBR": "Recomendação Inteligente", "ro": "Recomandare Inteligentă"
    },
    "detected_type": {
        "en": "Detected Type: %1$s", "de": "Erkannter Typ: %1$s", "es": "Tipo detectado: %1$s", 
        "fr": "Type détecté : %1$s", "in": "Tipe Terdeteksi: %1$s", "it": "Tipo rilevato: %1$s",
        "pt": "Tipo Detetado: %1$s", "pt-rBR": "Tipo Detectado: %1$s", "ro": "Tip Detectat: %1$s"
    },
    "doc_stats_pages_images": {
        "en": "Pages: %1$d | Images: %2$d", "de": "Seiten: %1$d | Bilder: %2$d", "es": "Páginas: %1$d | Imágenes: %2$d", 
        "fr": "Pages : %1$d | Images : %2$d", "in": "Halaman: %1$d | Gambar: %2$d", "it": "Pagine: %1$d | Immagini: %2$d",
        "pt": "Páginas: %1$d | Imagens: %2$d", "pt-rBR": "Páginas: %1$d | Imagens: %2$d", "ro": "Pagini: %1$d | Imagini: %2$d"
    },
    "warn_grayscale": {
        "en": "⚠️ Discards all color elements &amp; diagrams.", 
        "de": "⚠️ Verwirft alle Farbelemente &amp; Diagramme.", 
        "es": "⚠️ Descarta elementos de color y diagramas.", 
        "fr": "⚠️ Supprime les éléments en couleur et diagrammes.", 
        "in": "⚠️ Membuang elemen warna &amp; diagram.", 
        "it": "⚠️ Scarta tutti gli elementi a colori e i diagrammi.",
        "pt": "⚠️ Descarta todos os elementos a cores e diagramas.", 
        "pt-rBR": "⚠️ Descarta elementos de cor e diagramas.", 
        "ro": "⚠️ Elimină elementele color și diagramele."
    },
    "warn_lossless": {
        "en": "⚠️ Lossless on scans can significantly increase size.", 
        "de": "⚠️ Verlustfrei bei Scans kann die Größe stark erhöhen.", 
        "es": "⚠️ Sin pérdida en escaneos puede aumentar el tamaño.", 
        "fr": "⚠️ La compression sans perte peut augmenter la taille des scans.", 
        "in": "⚠️ Kompresi tanpa hilang pada pindaian dapat meningkatkan ukuran.", 
        "it": "⚠️ Senza perdita su scansioni può aumentare notevolmente le dimensioni.",
        "pt": "⚠️ Compressão sem perda em digitalizações pode aumentar o tamanho.", 
        "pt-rBR": "⚠️ Sem perda em escaneamentos pode aumentar bastante o tamanho.", 
        "ro": "⚠️ Comprimarea fără pierderi la scanări poate crește dimensiunea."
    },
    "warn_signatures": {
        "en": "⚠️ May invalidate digital signatures on official files.", 
        "de": "⚠️ Kann digitale Signaturen auf offiziellen Dateien ungültig machen.", 
        "es": "⚠️ Puede invalidar firmas digitales.", 
        "fr": "⚠️ Peut invalider les signatures numériques.", 
        "in": "⚠️ Mungkin membatalkan tanda tangan digital.", 
        "it": "⚠️ Potrebbe invalidare le firme digitali sui file ufficiali.",
        "pt": "⚠️ Pode invalidar assinaturas digitais em ficheiros oficiais.", 
        "pt-rBR": "⚠️ Pode invalidar assinaturas digitais em arquivos oficiais.", 
        "ro": "⚠️ Poate invalida semnăturile digitale."
    },
    "recent_activity": {
        "en": "Recent Activity", "de": "Letzte Aktivität", "es": "Actividad Reciente", 
        "fr": "Activité récente", "in": "Aktivitas Terbaru", "it": "Attività recente",
        "pt": "Atividade Recente", "pt-rBR": "Atividade Recente", "ro": "Activitate Recentă"
    },
    "premium_title": {
        "en": "Premium Upgrade", "de": "Premium-Upgrade", "es": "Actualización Premium", 
        "fr": "Mise à niveau Premium", "in": "Peningkatan Premium", "it": "Aggiornamento Premium",
        "pt": "Atualização Premium", "pt-rBR": "Upgrade Premium", "ro": "Upgrade Premium"
    },
    "premium_thanks": {
        "en": "Thank you for supporting our mission of privacy-first, offline document tools!", 
        "de": "Danke für die Unterstützung unserer Offline-Werkzeuge!", 
        "es": "¡Gracias por apoyar nuestras herramientas fuera de línea!", 
        "fr": "Merci de soutenir nos outils de documents hors ligne !", 
        "in": "Terima kasih telah mendukung alat offline kami!", 
        "it": "Grazie per supportare i nostri strumenti offline!",
        "pt": "Obrigado por apoiar as nossas ferramentas offline!", 
        "pt-rBR": "Obrigado por apoiar nossas ferramentas offline!", 
        "ro": "Mulțumim pentru susținerea uneltelor noastre offline!"
    },
    "premium_features": {
        "en": "Unlock the ultimate offline PDF experience.\n\n✓ No Banner Ads\n✓ No Interstitials\n✓ Unlimited Batch Actions", 
        "de": "Das ultimative PDF-Erlebnis freischalten.\n\n✓ Keine Banner-Werbung\n✓ Keine Interstitials\n✓ Unbegrenzte Batch-Aktionen", 
        "es": "Desbloquea la experiencia PDF definitiva.\n\n✓ Sin Anuncios\n✓ Sin Intersticiales\n✓ Acciones por Lotes Ilimitadas", 
        "fr": "Débloquez l'expérience PDF ultime.\n\n✓ Pas de publicités\n✓ Actions illimitées", 
        "in": "Buka pengalaman PDF terbaik.\n\n✓ Tanpa Iklan\n✓ Aksi Batch Tak Terbatas", 
        "it": "Sblocca l'esperienza PDF definitiva.\n\n✓ Nessuna pubblicità\n✓ Azioni in blocco illimitate",
        "pt": "Desbloqueie a derradeira experiência PDF.\n\n✓ Sem Anúncios\n✓ Sem Intersticiais\n✓ Ações em Lote Ilimitadas", 
        "pt-rBR": "Desbloqueie a melhor experiência PDF.\n\n✓ Sem Anúncios\n✓ Sem Intersticiais\n✓ Ações em Lote Ilimitadas", 
        "ro": "Deblochează experiența PDF supremă.\n\n✓ Fără Reclame\n✓ Fără Interstițiale\n✓ Acțiuni Nelimitate"
    },
    "batch_compressing": {
        "en": "Compressing %1$d of %2$d files...", 
        "de": "Komprimiere %1$d von %2$d Dateien...", 
        "es": "Comprimiendo %1$d de %2$d archivos...", 
        "fr": "Compression de %1$d sur %2$d fichiers...", 
        "in": "Mengompresi %1$d dari %2$d file...", 
        "it": "Compressione di %1$d su %2$d file...",
        "pt": "A comprimir %1$d de %2$d ficheiros...", 
        "pt-rBR": "Comprimindo %1$d de %2$d arquivos...", 
        "ro": "Se comprimă %1$d din %2$d fișiere..."
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
    
    # insert before </resources>
    insert_str = ""
    for key, trans_dict in translations.items():
        val = trans_dict.get(lang_code, trans_dict["en"])
        # Escape XML chars
        val = val.replace("'", "\\'").replace('"', '\\"')
        insert_str += f'    <string name="{key}">{val}</string>\n'
    
    new_content = content.replace("</resources>", insert_str + "</resources>")
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

print("Strings added to all XMLs")

ma_path = "app/src/main/java/com/example/shrinkpdf/MainActivity.kt"
with open(ma_path, 'r', encoding='utf-8') as f:
    ma_code = f.read()

replacements = [
    ('Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()',
     'Toast.makeText(context, context.getString(R.string.msg_failed_export), Toast.LENGTH_SHORT).show()'),
    
    ('message = "Scan saved to Repository",',
     'message = stringResource(R.string.msg_scan_saved),'),
    
    ('actionLabel = "Export",',
     'actionLabel = stringResource(R.string.action_export),'),
    
    ('snackbarHostState.showSnackbar("Failed to process scan")',
     'snackbarHostState.showSnackbar(context.getString(R.string.msg_failed_scan))'),
    
    ('title = "Scan to PDF",',
     'title = stringResource(R.string.menu_scan),'),
    
    ('subtitle = "Use the native document scanner to create a PDF",',
     'subtitle = stringResource(R.string.menu_scan_desc),'),
    
    ('title = "Images to PDF",',
     'title = stringResource(R.string.images_to_pdf),'),
    
    ('subtitle = "Convert JPG, PNG, and other images to a single PDF",',
     'subtitle = stringResource(R.string.subtitle_images_to_pdf),'),
    
    ('title = "Text Format Converter",',
     'title = stringResource(R.string.text_format_converter),'),
    
    ('subtitle = "Convert between TXT, MD, CSV, JSON, and more",',
     'subtitle = stringResource(R.string.subtitle_format_converter),'),
    
    ('title = "Compress PDF",',
     'title = stringResource(R.string.compress_title),'),
    
    ('subtitle = "Make PDFs smaller, safely.",',
     'subtitle = stringResource(R.string.cat_compress_desc),'),
    
    ('title = "Batch Compression",',
     'title = stringResource(R.string.compress_batch),'),
    
    ('subtitle = "Select a folder and compress all PDFs inside",',
     'subtitle = stringResource(R.string.subtitle_batch_compress),'),
    
    ('text = "PDFchemy Tools",',
     'text = stringResource(R.string.title_tools),'),
    
    ('text = "FIX DOCUMENTS LOCALLY",',
     'text = stringResource(R.string.home_subtitle),'),
    
    ('Text(text = "File Name: $sourceName", style = MaterialTheme.typography.bodyLarge)',
     'Text(text = stringResource(R.string.file_name_label, sourceName), style = MaterialTheme.typography.bodyLarge)'),
    
    ('text = "Selected Batch Files:",',
     'text = stringResource(R.string.selected_batch_files),'),
    
    ('text = "Analyzing document structure...",',
     'text = stringResource(R.string.analyzing_doc),'),
    
    ('text = "Smart Recommendation",',
     'text = stringResource(R.string.smart_recommendation),'),
    
    ('text = "Detected Type: ${analysis.scenario.displayName}",',
     'text = stringResource(R.string.detected_type, analysis.scenario.displayName),'),
    
    ('text = "Pages: ${analysis.pageCount} | Images: ${analysis.imageCount}",',
     'text = stringResource(R.string.doc_stats_pages_images, analysis.pageCount, analysis.imageCount),'),
    
    ('text = "?? Discards all color elements & diagrams.",',
     'text = stringResource(R.string.warn_grayscale),'),
    
    ('text = "?? Lossless on scans can significantly increase size.",',
     'text = stringResource(R.string.warn_lossless),'),
    
    ('text = "?? May invalidate digital signatures on official files.",',
     'text = stringResource(R.string.warn_signatures),'),
    
    ('text = "Recent Activity",',
     'text = stringResource(R.string.recent_activity),'),
    
    ('title = "Premium Upgrade",',
     'title = stringResource(R.string.premium_title),'),
    
    ('text = "Thank you for supporting our mission of privacy-first, offline document tools!",',
     'text = stringResource(R.string.premium_thanks),'),
    
    ('text = "Compressing ${state.current} of ${state.total} files...",',
     'text = stringResource(R.string.batch_compressing, state.current, state.total),'),
]

for old, new in replacements:
    ma_code = ma_code.replace(old, new)

# manual regex for Premium features due to multiple spaces and newlines
ma_code = re.sub(
    r'text\s*=\s*"Unlock the ultimate offline PDF experience\.\\n\\n.*?Unlimited Batch Actions"',
    r'text = stringResource(R.string.premium_features)',
    ma_code, flags=re.DOTALL
)

# Text to PDF replacement
ma_code = ma_code.replace('Text(text = "Text to PDF"', 'Text(text = stringResource(R.string.text_format_converter)')
ma_code = ma_code.replace('title = "Text to PDF"', 'title = stringResource(R.string.text_format_converter)')
ma_code = ma_code.replace('subtitle = "Convert your notes or .txt files to PDF",', 'subtitle = stringResource(R.string.subtitle_text_to_pdf),')

# Warn fix - the symbols are actually parsed differently maybe, let's just do a regex
ma_code = re.sub(
    r'text\s*=\s*".*?Discards all color elements & diagrams\.",',
    r'text = stringResource(R.string.warn_grayscale),',
    ma_code
)
ma_code = re.sub(
    r'text\s*=\s*".*?Lossless on scans can significantly increase size\.",',
    r'text = stringResource(R.string.warn_lossless),',
    ma_code
)
ma_code = re.sub(
    r'text\s*=\s*".*?May invalidate digital signatures on official files\.",',
    r'text = stringResource(R.string.warn_signatures),',
    ma_code
)

with open(ma_path, 'w', encoding='utf-8') as f:
    f.write(ma_code)

print("MainActivity.kt patched.")
