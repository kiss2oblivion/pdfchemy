import os

translations = {
    "title_compress_pdf": {
        "en": "Compress PDF", "de": "PDF komprimieren", "es": "Comprimir PDF",
        "fr": "Compresser PDF", "in": "Kompres PDF", "it": "Comprimi PDF",
        "pt": "Comprimir PDF", "pt-rBR": "Comprimir PDF", "ro": "Comprimare PDF"
    },
    "msg_compress_warning_signature": {
        "en": "Warning: Compressing may invalidate the digital signature.", "de": "Warnung: Die Komprimierung kann die digitale Signatur ungültig machen.", "es": "Advertencia: Comprimir puede invalidar la firma digital.",
        "fr": "Avertissement : La compression peut invalider la signature numérique.", "in": "Peringatan: Mengompresi dapat membatalkan tanda tangan digital.", "it": "Avviso: La compressione può invalidare la firma digitale.",
        "pt": "Aviso: Comprimir pode invalidar a assinatura digital.", "pt-rBR": "Aviso: Comprimir pode invalidar a assinatura digital.", "ro": "Avertisment: Comprimarea poate invalida semnătura digitală."
    },
    "msg_compress_slightly_larger": {
        "en": "The output is slightly larger than the original. This PDF may already be highly optimized.\\n\\n", "de": "Die Ausgabe ist geringfügig größer als das Original. Diese PDF ist möglicherweise bereits hoch optimiert.\\n\\n", "es": "La salida es ligeramente mayor que el original. Es posible que este PDF ya esté muy optimizado.\\n\\n",
        "fr": "La sortie est légèrement plus grande que l\\'original. Ce PDF est peut-être déjà hautement optimisé.\\n\\n", "in": "Outputnya sedikit lebih besar dari aslinya. PDF ini mungkin sudah sangat dioptimalkan.\\n\\n", "it": "L\\'output è leggermente più grande dell\\'originale. Questo PDF potrebbe essere già altamente ottimizzato.\\n\\n",
        "pt": "A saída é ligeiramente maior que o original. Este PDF pode já estar altamente otimizado.\\n\\n", "pt-rBR": "A saída é ligeiramente maior que o original. Este PDF pode já estar altamente otimizado.\\n\\n", "ro": "Ieșirea este puțin mai mare decât originalul. Acest PDF poate fi deja extrem de optimizat.\\n\\n"
    },
    "msg_compress_reduced": {
        "en": "Reduced by %1$d%% (%2$s saved!)\\n\\n", "de": "Reduziert um %1$d%% (%2$s gespart!)\\n\\n", "es": "Reducido un %1$d%% (¡%2$s ahorrado!)\\n\\n",
        "fr": "Réduit de %1$d%% (%2$s économisé !)\\n\\n", "in": "Berkurang sebesar %1$d%% (%2$s dihemat!)\\n\\n", "it": "Ridotto del %1$d%% (%2$s risparmiato!)\\n\\n",
        "pt": "Reduzido em %1$d%% (%2$s poupado!)\\n\\n", "pt-rBR": "Reduzido em %1$d%% (%2$s economizado!)\\n\\n", "ro": "Redus cu %1$d%% (%2$s economisit!)\\n\\n"
    },
    "msg_compress_verdict_no_benefit": {
        "en": "No compression benefit", "de": "Kein Komprimierungsvorteil", "es": "Sin beneficio de compresión",
        "fr": "Aucun avantage de compression", "in": "Tidak ada manfaat kompresi", "it": "Nessun vantaggio di compressione",
        "pt": "Sem benefício de compressão", "pt-rBR": "Sem benefício de compressão", "ro": "Fără beneficiu de comprimare"
    },
    "msg_compress_verdict_excellent": {
        "en": "Excellent compression", "de": "Ausgezeichnete Komprimierung", "es": "Excelente compresión",
        "fr": "Excellente compression", "in": "Kompresi sangat baik", "it": "Eccellente compressione",
        "pt": "Excelente compressão", "pt-rBR": "Excelente compressão", "ro": "Comprimare excelentă"
    },
    "msg_compress_verdict_good": {
        "en": "Good compression", "de": "Gute Komprimierung", "es": "Buena compresión",
        "fr": "Bonne compression", "in": "Kompresi yang bagus", "it": "Buona compressione",
        "pt": "Boa compressão", "pt-rBR": "Boa compressão", "ro": "Comprimare bună"
    },
    "msg_compress_verdict_minor": {
        "en": "Minor compression", "de": "Geringfügige Komprimierung", "es": "Compresión menor",
        "fr": "Compression mineure", "in": "Kompresi kecil", "it": "Compressione minore",
        "pt": "Compressão menor", "pt-rBR": "Compressão menor", "ro": "Comprimare minoră"
    },
    "msg_compress_verdict_minimal": {
        "en": "Minimal compression gain", "de": "Minimaler Komprimierungsgewinn", "es": "Ganancia de compresión mínima",
        "fr": "Gain de compression minimal", "in": "Keuntungan kompresi minimal", "it": "Guadagno di compressione minimo",
        "pt": "Ganho de compressão mínimo", "pt-rBR": "Ganho de compressão mínimo", "ro": "Câștig de comprimare minim"
    },
    "msg_compress_verdict_format": {
        "en": "Verdict: %1$s\\n\\n", "de": "Urteil: %1$s\\n\\n", "es": "Veredicto: %1$s\\n\\n",
        "fr": "Verdict : %1$s\\n\\n", "in": "Putusan: %1$s\\n\\n", "it": "Verdetto: %1$s\\n\\n",
        "pt": "Veredicto: %1$s\\n\\n", "pt-rBR": "Veredicto: %1$s\\n\\n", "ro": "Verdict: %1$s\\n\\n"
    },
    "msg_compress_original_size": {
        "en": "Original Size: %1$s\\n", "de": "Originalgröße: %1$s\\n", "es": "Tamaño original: %1$s\\n",
        "fr": "Taille d\\'origine : %1$s\\n", "in": "Ukuran Asli: %1$s\\n", "it": "Dimensione originale: %1$s\\n",
        "pt": "Tamanho Original: %1$s\\n", "pt-rBR": "Tamanho Original: %1$s\\n", "ro": "Dimensiune Originală: %1$s\\n"
    },
    "msg_compress_compressed_size": {
        "en": "Compressed Size: %1$s\\n\\n", "de": "Komprimierte Größe: %1$s\\n\\n", "es": "Tamaño comprimido: %1$s\\n\\n",
        "fr": "Taille compressée : %1$s\\n\\n", "in": "Ukuran Kompresi: %1$s\\n\\n", "it": "Dimensione compressa: %1$s\\n\\n",
        "pt": "Tamanho Comprimido: %1$s\\n\\n", "pt-rBR": "Tamanho Comprimido: %1$s\\n\\n", "ro": "Dimensiune Comprimată: %1$s\\n\\n"
    },
    "msg_compress_settings_used": {
        "en": "Settings Used:\\n", "de": "Verwendete Einstellungen:\\n", "es": "Configuración utilizada:\\n",
        "fr": "Paramètres utilisés :\\n", "in": "Pengaturan yang Digunakan:\\n", "it": "Impostazioni usate:\\n",
        "pt": "Definições Utilizadas:\\n", "pt-rBR": "Configurações Usadas:\\n", "ro": "Setări Utilizate:\\n"
    },
    "msg_compress_target_size": {
        "en": "- Target Size: %1$s MB\\n", "de": "- Zielgröße: %1$s MB\\n", "es": "- Tamaño objetivo: %1$s MB\\n",
        "fr": "- Taille cible : %1$s Mo\\n", "in": "- Ukuran Target: %1$s MB\\n", "it": "- Dimensione obiettivo: %1$s MB\\n",
        "pt": "- Tamanho Alvo: %1$s MB\\n", "pt-rBR": "- Tamanho Alvo: %1$s MB\\n", "ro": "- Dimensiune Țintă: %1$s MB\\n"
    },
    "msg_compress_auto_optimized": {
        "en": "- Auto-Optimized: Yes\\n", "de": "- Automatisch optimiert: Ja\\n", "es": "- Optimizado automáticamente: Sí\\n",
        "fr": "- Optimisation automatique : Oui\\n", "in": "- Dioptimalkan Otomatis: Ya\\n", "it": "- Auto-ottimizzato: Sì\\n",
        "pt": "- Auto-otimizado: Sim\\n", "pt-rBR": "- Auto-otimizado: Sim\\n", "ro": "- Auto-optimizat: Da\\n"
    },
    "msg_compress_quality_preset": {
        "en": "- Quality Preset: %1$d%%\\n", "de": "- Qualitätsvoreinstellung: %1$d%%\\n", "es": "- Preajuste de calidad: %1$d%%\\n",
        "fr": "- Préréglage de qualité : %1$d%%\\n", "in": "- Prasetel Kualitas: %1$d%%\\n", "it": "- Preset qualità: %1$d%%\\n",
        "pt": "- Predefinição de Qualidade: %1$d%%\\n", "pt-rBR": "- Predefinição de Qualidade: %1$d%%\\n", "ro": "- Presetare Calitate: %1$d%%\\n"
    },
    "msg_compress_grayscale": {
        "en": "- Grayscale: %1$s\\n", "de": "- Graustufen: %1$s\\n", "es": "- Escala de grises: %1$s\\n",
        "fr": "- Niveaux de gris : %1$s\\n", "in": "- Skala abu-abu: %1$s\\n", "it": "- Scala di grigi: %1$s\\n",
        "pt": "- Escala de cinzentos: %1$s\\n", "pt-rBR": "- Escala de cinza: %1$s\\n", "ro": "- Tonuri de gri: %1$s\\n"
    },
    "msg_compress_lossless_zip": {
        "en": "- Lossless ZIP: %1$s\\n", "de": "- Verlustfreies ZIP: %1$s\\n", "es": "- ZIP sin pérdida: %1$s\\n",
        "fr": "- ZIP sans perte : %1$s\\n", "in": "- ZIP tanpa kerugian: %1$s\\n", "it": "- ZIP senza perdita: %1$s\\n",
        "pt": "- ZIP sem perdas: %1$s\\n", "pt-rBR": "- ZIP sem perdas: %1$s\\n", "ro": "- ZIP fără pierderi: %1$s\\n"
    },
    "msg_compress_metadata_removed": {
        "en": "- Metadata Removed: %1$s", "de": "- Metadaten entfernt: %1$s", "es": "- Metadatos eliminados: %1$s",
        "fr": "- Métadonnées supprimées : %1$s", "in": "- Metadata Dihapus: %1$s", "it": "- Metadati rimossi: %1$s",
        "pt": "- Metadados Removidos: %1$s", "pt-rBR": "- Metadados Removidos: %1$s", "ro": "- Metadate Eliminate: %1$s"
    },
    "msg_compress_enabled": {
        "en": "Enabled", "de": "Aktiviert", "es": "Habilitado",
        "fr": "Activé", "in": "Diaktifkan", "it": "Abilitato",
        "pt": "Ativado", "pt-rBR": "Ativado", "ro": "Activat"
    },
    "msg_compress_disabled": {
        "en": "Disabled", "de": "Deaktiviert", "es": "Deshabilitado",
        "fr": "Désactivé", "in": "Dinonaktifkan", "it": "Disabilitato",
        "pt": "Desativado", "pt-rBR": "Desativado", "ro": "Dezactivat"
    },
    "msg_compress_success": {
        "en": "Compression finished successfully.\\n\\n", "de": "Komprimierung erfolgreich abgeschlossen.\\n\\n", "es": "Compresión finalizada con éxito.\\n\\n",
        "fr": "Compression terminée avec succès.\\n\\n", "in": "Kompresi berhasil diselesaikan.\\n\\n", "it": "Compressione terminata con successo.\\n\\n",
        "pt": "Compressão concluída com sucesso.\\n\\n", "pt-rBR": "Compressão concluída com sucesso.\\n\\n", "ro": "Comprimare finalizată cu succes.\\n\\n"
    },
    "msg_compress_limit_reached": {
        "en": "The best possible compression was applied, but the file could not be compressed under %1$s MB without destroying the content.\\n\\n", "de": "Die bestmögliche Komprimierung wurde angewendet, aber die Datei konnte nicht unter %1$s MB komprimiert werden, ohne den Inhalt zu zerstören.\\n\\n", "es": "Se aplicó la mejor compresión posible, pero el archivo no pudo comprimirse por debajo de %1$s MB sin destruir el contenido.\\n\\n",
        "fr": "La meilleure compression possible a été appliquée, mais le fichier n\\'a pas pu être compressé sous %1$s Mo sans détruire le contenu.\\n\\n", "in": "Kompresi terbaik yang mungkin telah diterapkan, tetapi file tidak dapat dikompresi di bawah %1$s MB tanpa merusak konten.\\n\\n", "it": "È stata applicata la migliore compressione possibile, ma il file non poteva essere compresso sotto %1$s MB senza distruggere il contenuto.\\n\\n",
        "pt": "A melhor compressão possível foi aplicada, mas o ficheiro não pôde ser comprimido para menos de %1$s MB sem destruir o conteúdo.\\n\\n", "pt-rBR": "A melhor compressão possível foi aplicada, mas o arquivo não pôde ser comprimido para menos de %1$s MB sem destruir o conteúdo.\\n\\n", "ro": "A fost aplicată cea mai bună comprimare posibilă, dar fișierul nu a putut fi comprimat sub %1$s MB fără a distruge conținutul.\\n\\n"
    },
    "title_compress_result": {
        "en": "Compression Result", "de": "Komprimierungsergebnis", "es": "Resultado de la compresión",
        "fr": "Résultat de la compression", "in": "Hasil Kompresi", "it": "Risultato della compressione",
        "pt": "Resultado da Compressão", "pt-rBR": "Resultado da Compressão", "ro": "Rezultat Comprimare"
    },
    "title_target_size_unreachable": {
        "en": "Target Size Unreachable", "de": "Zielgröße nicht erreichbar", "es": "Tamaño objetivo inalcanzable",
        "fr": "Taille cible inaccessible", "in": "Ukuran Target Tidak Terjangkau", "it": "Dimensione obiettivo irraggiungibile",
        "pt": "Tamanho Alvo Inalcançável", "pt-rBR": "Tamanho Alvo Inalcançável", "ro": "Dimensiune Țintă Neaccesibilă"
    },
    "label_compressed_pdf": {
        "en": "Compressed PDF", "de": "Komprimiertes PDF", "es": "PDF comprimido",
        "fr": "PDF compressé", "in": "PDF terkompresi", "it": "PDF compresso",
        "pt": "PDF comprimido", "pt-rBR": "PDF comprimido", "ro": "PDF comprimat"
    },
    "action_compress": {
        "en": "Compress", "de": "Komprimieren", "es": "Comprimir",
        "fr": "Compresser", "in": "Kompres", "it": "Comprimi",
        "pt": "Comprimir", "pt-rBR": "Comprimir", "ro": "Comprimați"
    },
    "msg_error_unknown": {
        "en": "An unknown error occurred.", "de": "Ein unbekannter Fehler ist aufgetreten.", "es": "Se produjo un error desconocido.",
        "fr": "Une erreur inconnue s\\'est produite.", "in": "Terjadi kesalahan yang tidak diketahui.", "it": "Si è verificato un errore sconosciuto.",
        "pt": "Ocorreu um erro desconhecido.", "pt-rBR": "Ocorreu um erro desconhecido.", "ro": "A apărut o eroare necunoscută."
    },
    "msg_no_files_batch": {
        "en": "No files selected for batch compression.", "de": "Keine Dateien für die Stapelkomprimierung ausgewählt.", "es": "No hay archivos seleccionados para la compresión por lotes.",
        "fr": "Aucun fichier sélectionné pour la compression par lots.", "in": "Tidak ada file yang dipilih untuk kompresi batch.", "it": "Nessun file selezionato per la compressione in blocco.",
        "pt": "Nenhum ficheiro selecionado para compressão em lote.", "pt-rBR": "Nenhum arquivo selecionado para compressão em lote.", "ro": "Niciun fișier selectat pentru comprimarea în lot."
    },
    "msg_invalid_folder": {
        "en": "Selected folder is invalid or does not exist.", "de": "Der ausgewählte Ordner ist ungültig oder existiert nicht.", "es": "La carpeta seleccionada no es válida o no existe.",
        "fr": "Le dossier sélectionné n\\'est pas valide ou n\\'existe pas.", "in": "Folder yang dipilih tidak valid atau tidak ada.", "it": "La cartella selezionata non è valida o non esiste.",
        "pt": "A pasta selecionada é inválida ou não existe.", "pt-rBR": "A pasta selecionada é inválida ou não existe.", "ro": "Dosarul selectat nu este valid sau nu există."
    },
    "label_batch_compress_folder": {
        "en": "Batch Compression Folder", "de": "Stapelkomprimierungsordner", "es": "Carpeta de compresión por lotes",
        "fr": "Dossier de compression par lots", "in": "Folder Kompresi Batch", "it": "Cartella di compressione in blocco",
        "pt": "Pasta de Compressão em Lote", "pt-rBR": "Pasta de Compressão em Lote", "ro": "Dosar Comprimare în Lot"
    },
    "action_compress_batch": {
        "en": "Compress Batch", "de": "Stapel komprimieren", "es": "Comprimir lote",
        "fr": "Compresser le lot", "in": "Kompres Batch", "it": "Comprimi blocco",
        "pt": "Comprimir Lote", "pt-rBR": "Comprimir Lote", "ro": "Comprimare Lot"
    },
    "title_batch_compress_result": {
        "en": "Batch Compression Finished", "de": "Stapelkomprimierung abgeschlossen", "es": "Compresión por lotes finalizada",
        "fr": "Compression par lots terminée", "in": "Kompresi Batch Selesai", "it": "Compressione in blocco terminata",
        "pt": "Compressão em Lote Concluída", "pt-rBR": "Compressão em Lote Concluída", "ro": "Comprimare în Lot Finalizată"
    },
    "msg_batch_compress_success_all": {
        "en": "Successfully compressed all %1$d files!\\nTotal space saved: %2$s", "de": "Erfolgreich alle %1$d Dateien komprimiert!\\nGespartes Speichervolumen: %2$s", "es": "¡Se comprimieron con éxito los %1$d archivos!\\nEspacio total ahorrado: %2$s",
        "fr": "Compression réussie de tous les %1$d fichiers !\\nEspace total économisé : %2$s", "in": "Berhasil mengompresi semua %1$d file!\\nTotal ruang yang dihemat: %2$s", "it": "Tutti i %1$d file compressi con successo!\\nSpazio totale risparmiato: %2$s",
        "pt": "Todos os %1$d ficheiros comprimidos com sucesso!\\nEspaço total poupado: %2$s", "pt-rBR": "Todos os %1$d arquivos comprimidos com sucesso!\\nEspaço total economizado: %2$s", "ro": "Au fost comprimate cu succes toate cele %1$d fișiere!\\nSpațiu total economisit: %2$s"
    },
    "msg_batch_compress_success_partial": {
        "en": "Successfully compressed %1$d of %2$d files.\\nTotal space saved: %3$s", "de": "Erfolgreich %1$d von %2$d Dateien komprimiert.\\nGespartes Speichervolumen: %3$s", "es": "Se comprimieron con éxito %1$d de %2$d archivos.\\nEspacio total ahorrado: %3$s",
        "fr": "Compression réussie de %1$d sur %2$d fichiers.\\nEspace total économisé : %3$s", "in": "Berhasil mengompresi %1$d dari %2$d file.\\nTotal ruang yang dihemat: %3$s", "it": "Compressi con successo %1$d di %2$d file.\\nSpazio totale risparmiato: %3$s",
        "pt": "Comprimidos com sucesso %1$d de %2$d ficheiros.\\nEspaço total poupado: %3$s", "pt-rBR": "Comprimidos com sucesso %1$d de %2$d arquivos.\\nEspaço total economizado: %3$s", "ro": "Au fost comprimate cu succes %1$d din %2$d fișiere.\\nSpațiu total economisit: %3$s"
    },
    "msg_batch_compress_fail": {
        "en": "Failed to compress any files in the batch.", "de": "Fehler beim Komprimieren von Dateien im Stapel.", "es": "No se pudo comprimir ningún archivo en el lote.",
        "fr": "Échec de la compression des fichiers du lot.", "in": "Gagal mengompresi file apa pun dalam batch.", "it": "Impossibile comprimere alcun file nel blocco.",
        "pt": "Falha ao comprimir quaisquer ficheiros no lote.", "pt-rBR": "Falha ao comprimir quaisquer arquivos no lote.", "ro": "Nu s-a putut comprima niciun fișier din lot."
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

import re

# Fix MainActivity.kt
ma_path = "app/src/main/java/com/example/shrinkpdf/MainActivity.kt"
with open(ma_path, 'r', encoding='utf-8') as f:
    ma_code = f.read()
ma_code = ma_code.replace('PremiumTopAppBar("Compress PDF"', 'PremiumTopAppBar(stringResource(R.string.title_compress_pdf)')
with open(ma_path, 'w', encoding='utf-8') as f:
    f.write(ma_code)

# Fix MainViewModel.kt
vm_path = "app/src/main/java/com/example/shrinkpdf/ui/MainViewModel.kt"
with open(vm_path, 'r', encoding='utf-8') as f:
    vm_code = f.read()

# Make the big multi-line changes:
vm_code = vm_code.replace('"Warning: Compressing may invalidate the digital signature."', 'context.getString(R.string.msg_compress_warning_signature)')
vm_code = vm_code.replace('"The output is slightly larger than the original. This PDF may already be highly optimized.\\n\\n"', 'context.getString(R.string.msg_compress_slightly_larger)')
vm_code = vm_code.replace('"Reduced by $reductionPercent% ($saved saved!)\\n\\n"', 'context.getString(R.string.msg_compress_reduced, reductionPercent, saved)')

vm_code = vm_code.replace('"No compression benefit"', 'context.getString(R.string.msg_compress_verdict_no_benefit)')
vm_code = vm_code.replace('"Excellent compression"', 'context.getString(R.string.msg_compress_verdict_excellent)')
vm_code = vm_code.replace('"Good compression"', 'context.getString(R.string.msg_compress_verdict_good)')
vm_code = vm_code.replace('"Minor compression"', 'context.getString(R.string.msg_compress_verdict_minor)')
vm_code = vm_code.replace('"Minimal compression gain"', 'context.getString(R.string.msg_compress_verdict_minimal)')

vm_code = vm_code.replace('"Verdict: $verdict\\n\\n"', 'context.getString(R.string.msg_compress_verdict_format, verdict)')
vm_code = vm_code.replace('"Original Size: ${formatSize(originalSize)}\\n"', 'context.getString(R.string.msg_compress_original_size, formatSize(originalSize))')
vm_code = vm_code.replace('"Compressed Size: ${formatSize(compressedSize)}\\n\\n"', 'context.getString(R.string.msg_compress_compressed_size, formatSize(compressedSize))')
vm_code = vm_code.replace('"Settings Used:\\n"', 'context.getString(R.string.msg_compress_settings_used)')

vm_code = vm_code.replace('"- Target Size: ${_targetMb.value} MB\\n"', 'context.getString(R.string.msg_compress_target_size, _targetMb.value.toString())')
vm_code = vm_code.replace('"- Auto-Optimized: Yes\\n"', 'context.getString(R.string.msg_compress_auto_optimized)')
vm_code = vm_code.replace('"- Quality Preset: ${(compressionQuality.value * 100).toInt()}%\\n"', 'context.getString(R.string.msg_compress_quality_preset, (compressionQuality.value * 100).toInt())')

vm_code = vm_code.replace('"- Grayscale: ${if (useGrayscale.value) "Enabled" else "Disabled"}\\n"', 'context.getString(R.string.msg_compress_grayscale, if (useGrayscale.value) context.getString(R.string.msg_compress_enabled) else context.getString(R.string.msg_compress_disabled))')
vm_code = vm_code.replace('"- Lossless ZIP: ${if (useLossless.value) "Enabled" else "Disabled"}\\n"', 'context.getString(R.string.msg_compress_lossless_zip, if (useLossless.value) context.getString(R.string.msg_compress_enabled) else context.getString(R.string.msg_compress_disabled))')
vm_code = vm_code.replace('"- Metadata Removed: ${if (stripMetadata.value) "Yes" else "No"}"', 'context.getString(R.string.msg_compress_metadata_removed, if (stripMetadata.value) context.getString(R.string.value_yes) else context.getString(R.string.value_no))')

vm_code = vm_code.replace('"Compression finished successfully.\\n\\n"', 'context.getString(R.string.msg_compress_success)')

vm_code = vm_code.replace('"Target Size Unreachable"', 'context.getString(R.string.title_target_size_unreachable)')
vm_code = vm_code.replace('"The best possible compression was applied, but the file could not be compressed under ${_targetMb.value} MB without destroying the content.\\n\\n" + reductionDetails', 'context.getString(R.string.msg_compress_limit_reached, _targetMb.value.toString()) + reductionDetails')

vm_code = vm_code.replace('"Compression Result"', 'context.getString(R.string.title_compress_result)')
vm_code = vm_code.replace('"Compressed PDF"', 'context.getString(R.string.label_compressed_pdf)')
vm_code = vm_code.replace('historyRepository.addHistoryItem(destUri, context.getString(R.string.label_compressed_pdf), "Compress")', 'historyRepository.addHistoryItem(destUri, context.getString(R.string.label_compressed_pdf), context.getString(R.string.action_compress))')
vm_code = vm_code.replace('historyRepository.addHistoryItem(destUri, "Compressed PDF", "Compress")', 'historyRepository.addHistoryItem(destUri, context.getString(R.string.label_compressed_pdf), context.getString(R.string.action_compress))')
vm_code = vm_code.replace('"An unknown error occurred."', 'context.getString(R.string.msg_error_unknown)')

vm_code = vm_code.replace('"No files selected for batch compression."', 'context.getString(R.string.msg_no_files_batch)')
vm_code = vm_code.replace('"Selected folder is invalid or does not exist."', 'context.getString(R.string.msg_invalid_folder)')

vm_code = vm_code.replace('"Batch Compression Folder"', 'context.getString(R.string.label_batch_compress_folder)')
vm_code = vm_code.replace('"Compress Batch"', 'context.getString(R.string.action_compress_batch)')

vm_code = vm_code.replace('"Batch Compression Finished"', 'context.getString(R.string.title_batch_compress_result)')
vm_code = vm_code.replace('"Successfully compressed all $total files!\\nTotal space saved: $totalSavedStr"', 'context.getString(R.string.msg_batch_compress_success_all, total, totalSavedStr)')
vm_code = vm_code.replace('"Successfully compressed $successCount of $total files.\\nTotal space saved: $totalSavedStr"', 'context.getString(R.string.msg_batch_compress_success_partial, successCount, total, totalSavedStr)')
vm_code = vm_code.replace('"Failed to compress any files in the batch."', 'context.getString(R.string.msg_batch_compress_fail)')

with open(vm_path, 'w', encoding='utf-8') as f:
    f.write(vm_code)

print("Patch 5 applied.")
