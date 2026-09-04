import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

translations_v8 = {
    "en-US": """What's New in v2.0.1:
• Cross-Platform Compatibility: Seamless file sharing with Windows & Linux PDFchemy editions.
• Performance & Speed: Faster multi-page rendering, instant thumbnail caching, and lower memory footprint.
• UI & Display Polish: High-contrast accessibility improvements and fluid edge-to-edge navigation.
• 100% Offline & Private: Zero cloud uploads, zero telemetry, fully localized in 20 languages.""",

    "en-GB": """What's New in v2.0.1:
• Cross-Platform Compatibility: Seamless file sharing with Windows & Linux PDFchemy editions.
• Performance & Speed: Faster multi-page rendering, instant thumbnail caching, and lower memory footprint.
• UI & Display Polish: High-contrast accessibility improvements and fluid edge-to-edge navigation.
• 100% Offline & Private: Zero cloud uploads, zero telemetry, fully localized in 20 languages.""",

    "ro": """Noutăți în v2.0.1:
• Compatibilitate Multiplatformă: Schimb facil de fișiere cu versiunile PDFchemy pentru Windows și Linux.
• Performanță și Viteză: Randare mai rapidă a paginilor, încărcare instantanee a miniaturilor și consum redus de memorie.
• Îmbunătățiri de Afișaj: Contrast ridicat conform standardelor WCAG și navigare fluidă edge-to-edge.
• 100% Local și Privat: Zero încărcări în cloud, zero telemetrie, complet tradus în 20 de limbi.""",

    "de-DE": """Neu in v2.0.1:
• Plattformübergreifende Kompatibilität: Nahtloser Dateiaustausch mit Windows- & Linux-Versionen.
• Höhere Leistung: Schnellere Seitendarstellung, sofortiges Thumbnail-Caching und geringere Speichernutzung.
• UI- & Barrierefreiheits-Update: Besserer Kontrast und flüssige Edge-to-Edge-Navigation.
• 100% Offline & Sicher: Keine Cloud-Uploads, keine Telemetrie, vollständig in 20 Sprachen.""",

    "fr-FR": """Nouveautés de la v2.0.1 :
• Compatibilité multiplateforme : Partage fluide de fichiers avec les versions Windows et Linux de PDFchemy.
• Performances accrues : Rendu plus rapide des pages, mise en cache instantanée et mémoire optimisée.
• Améliorations de l'interface : Meilleur contraste d'accessibilité et navigation fluide bord à bord.
• 100% Hors-ligne et Privé : Zéro transfert vers le cloud, zéro télémétrie, traduit en 20 langues.""",

    "es-ES": """Novedades en v2.0.1:
• Compatibilidad multiplataforma: Intercambio fluido con las ediciones de Windows y Linux.
• Mayor rendimiento y velocidad: Renderizado de páginas más rápido y carga instantánea de miniaturas.
• Mejoras en la interfaz: Contraste optimizado y navegación fluida de borde a borde.
• 100% Local y Privado: Sin subidas a la nube, sin telemetría, disponible en 20 idiomas.""",

    "es-419": """Novedades en v2.0.1:
• Compatibilidad multiplataforma: Intercambio fluido con las ediciones de Windows y Linux.
• Mayor rendimiento y velocidad: Renderizado de páginas más rápido y carga instantánea de miniaturas.
• Mejoras en la interfaz: Contraste optimizado y navegación fluida de borde a borde.
• 100% Local y Privado: Sin subidas a la nube, sin telemetría, disponible en 20 idiomas.""",

    "es-US": """Novedades en v2.0.1:
• Compatibilidad multiplataforma: Intercambio fluido con las ediciones de Windows y Linux.
• Mayor rendimiento y velocidad: Renderizado de páginas más rápido y carga instantánea de miniaturas.
• Mejoras en la interfaz: Contraste optimizado y navegación fluida de borde a borde.
• 100% Local y Privado: Sin subidas a la nube, sin telemetría, disponible en 20 idiomas.""",

    "it-IT": """Novità nella v2.0.1:
• Compatibilità Multipiattaforma: Condivisione fluida con le versioni Windows e Linux.
• Prestazioni Migliorate: Rendering più veloce delle pagine e caricamento immediato delle miniature.
• Rifiniture Grafiche: Contrasto elevato per l'accessibilità e navigazione edge-to-edge fluida.
• 100% Offline e Privato: Nessun caricamento sul cloud, nessuna telemetria, tradotto in 20 lingue.""",

    "pt-BR": """Novidades na v2.0.1:
• Compatibilidade Multiplataforma: Integração perfeita com as versões para Windows e Linux.
• Maior Desempenho e Rapidez: Renderização mais rápida de páginas e cache instantâneo de miniaturas.
• Melhorias na Interface: Alto contraste para acessibilidade e navegação fluida de ponta a ponta.
• 100% Offline e Seguro: Zero uploads para nuvem, zero telemetria, traduzido em 20 idiomas.""",

    "pt-PT": """Novidades na v2.0.1:
• Compatibilidade Multiplataforma: Integração perfeita com as versões para Windows e Linux.
• Maior Desempenho e Rapidez: Renderização mais rápida de páginas e cache instantâneo de miniaturas.
• Melhorias na Interface: Alto contraste para acessibilidade e navegação fluida de ponta a ponta.
• 100% Offline e Seguro: Zero uploads para nuvem, zero telemetria, traduzido em 20 idiomas.""",

    "pl-PL": """Nowości w v2.0.1:
• Zgodność międzyplatformowa: Bezproblemowa wymiana plików z edycjami Windows i Linux.
• Wyższa wydajność: Szybsze renderowanie stron, natychmiastowe miniatury i niższe zużycie pamięci.
• Usprawnienia interfejsu: Lepszy kontrast i płynna nawigacja krawędź do krawędzi.
• 100% Offline i Prywatnie: Zero chmury, zero telemetrii, pełna lokalizacja w 20 językach.""",

    "nl-NL": """Nieuw in v2.0.1:
• Platformonafhankelijke compatibiliteit: Naadloze bestandsuitwisseling met Windows en Linux.
• Betere prestaties: Snellere paginageneratie, directe thumbnail-caching en lager geheugengebruik.
• Verbeterde interface: Verbeterd contrast voor toegankelijkheid en vloeiende edge-to-edge navigatie.
• 100% Offline & Privé: Geen cloud-uploads, geen telemetrie, volledig in 20 talen.""",

    "ru-RU": """Что нового в v2.0.1:
• Кроссплатформенность: Полная совместимость файлов с версиями для Windows и Linux.
• Повышенная скорость: Быстрый рендеринг страниц, мгновенные миниатюры и оптимизация памяти.
• Улучшения интерфейса: Повышенная контрастность и плавная навигация edge-to-edge.
• 100% Офлайн и Приватно: Никаких облачных загрузок, никакой телеметрии, 20 языков.""",

    "tr-TR": """v2.0.1 ile Gelen Yenilikler:
• Çapraz Platform Uyumluluğu: Windows ve Linux sürümleriyle sorunsuz dosya paylaşımı.
• Gelişmiş Performans: Daha hızlı sayfa işleme, anlık küçük resim önbelleği ve optimize bellek.
• Arayüz İyileştirmeleri: Yüksek kontrastlı erişilebilirlik ve akıcı kenardan kenara gezinme.
• %100 Çevrimdışı ve Güvenli: Buluta yükleme yok, telemetri yok, 20 dilde tam destek.""",

    "ar": """الجديد في الإصدار 2.0.1:
• التوافق مع المنصات: تبادل سلس للملفات مع إصدارات Windows و Linux.
• أداء وسرعة أكبر: معالجة أسرع للصفحات وتحميل فوري للمصغرات مع استهلاك أقل للذاكرة.
• تحسينات الواجهة: تباين عالي لسهولة القراءة وتصفح انسيابي من الحافة إلى الحافة.
• محلي وغير متصل 100%: بدون رفع إلى السحابة، بدون تتبع، متوفر بـ 20 لغة.""",

    "hi-IN": """v2.0.1 में नया क्या है:
• क्रॉस-प्लेटफ़ॉर्म अनुकूलता: Windows और Linux संस्करणों के साथ सहज फ़ाइल साझाकरण।
• बेहतर गति और प्रदर्शन: पृष्ठों की तेज़ रेंडरिंग और त्वरित थंबनेल लोडिंग।
• इंटरफ़ेस में सुधार: बेहतर कंट्रास्ट और सहज एज-टू-एज नेविगेशन।
• 100% ऑफ़लाइन और निजी: कोई क्लाउड अपलोड नहीं, कोई ट्रैकिंग नहीं, 20 भाषाओं में उपलब्ध।""",

    "id": """Yang Baru di v2.0.1:
• Kompatibilitas Lintas Platform: Berbagi file tanpa hambatan dengan edisi Windows & Linux.
• Peningkatan Kinerja: Render halaman lebih cepat, cache thumbnail instan, dan hemat memori.
• Pemolesan Antarmuka: Kontras aksesibilitas lebih baik dan navigasi edge-to-edge yang mulus.
• 100% Offline & Privat: Tanpa unggahan cloud, tanpa telemetri, didukung dalam 20 bahasa.""",

    "th": """มีอะไรใหม่ใน v2.0.1:
• ความเข้ากันได้ข้ามแพลตฟอร์ม: แชร์ไฟล์ได้อย่างราบรื่นกับเวอร์ชัน Windows และ Linux
• ประสิทธิภาพและความเร็ว: เรนเดอร์หน้าเอกสารเร็วขึ้น แคชภาพขนาดย่อทันที และประหยัดหน่วยความจำ
• ปรับปรุงอินเทอร์เฟซ: คอนทราสต์ชัดเจนยิ่งขึ้น และการนำทางแบบชิดขอบจออย่างลื่นไหล
• ออฟไลน์และเป็นส่วนตัว 100%: ไม่มีการอัปโหลดขึ้นคลาวด์ ไม่มีการติดตาม รองรับ 20 ภาษา""",

    "vi": """Có gì mới trong v2.0.1:
• Tương thích đa nền tảng: Chia sẻ tệp mượt mà với các phiên bản Windows và Linux.
• Hiệu năng và tốc độ: Kết xuất trang nhanh hơn, lưu ảnh thu nhỏ tức thì và tối ưu bộ nhớ.
• Tinh chỉnh giao diện: Tăng độ tương phản hỗ trợ tiếp cận và điều hướng tràn viền mượt mà.
• 100% Ngoại tuyến & Riêng tư: Không tải lên đám mây, không theo dõi, hỗ trợ 20 ngôn ngữ.""",

    "ja-JP": """v2.0.1 の新機能:
• マルチプラットフォーム対応: Windows および Linux 版とのシームレスなファイル連携。
• パフォーマンス向上: ページ描画の高速化、サムネイルの即時キャッシュ、メモリの最適化。
• UIと操作性の改善: ハイコントラスト表示への対応と滑らかな全画面エッジ・トゥ・エッジ操作。
• 100% オフライン＆プライバシー保護: クラウド送信なし、追跡なし、20言語に完全対応。""",

    "ko-KR": """v2.0.1 새로운 기능:
• 크로스 플랫폼 호환성: Windows 및 Linux 버전과 원활한 파일 호환.
• 성능 및 속도 향상: 더 빠른 페이지 렌더링, 즉각적인 섬네일 캐싱 및 메모리 최적화.
• UI 및 접근성 개선: 고대비 가독성 향상 및 부드러운 엣지-투-엣지 화면 제스처.
• 100% 오프라인 및 개인정보 보호: 클라우드 업로드 없음, 데이터 수집 없음, 20개 언어 지원。""",

    "zh-CN": """v2.0.1 更新内容:
• 跨平台无缝协同：与 Windows 及 Linux 桌面版 PDFchemy 完美兼容。
• 性能与渲染加速：多页渲染速度提升，缩略图即时缓存，内存占用更低。
• 界面与无障碍优化：高对比度视觉增强，手势沉浸式全面屏无缝导航。
• 100% 离线与隐私保护：零云端上传，零遥测，全面支持 20 种语言。""",

    "zh-TW": """v2.0.1 更新內容:
• 跨平台無縫協同：與 Windows 及 Linux 桌面版 PDFchemy 完美相容。
• 效能與渲染加速：多頁渲染速度提升，縮圖即時快取，記憶體佔用更低。
• 介面與無障礙優化：高對比視覺增強，手勢沉浸式全螢幕導航。
• 100% 離線與隱私保護：零雲端上傳，零追蹤，完整支援 20 種語言。"""
}

base_dir = "store_translations"
os.makedirs(base_dir, exist_ok=True)

for lang, text in translations_v8.items():
    lang_dir = os.path.join(base_dir, lang)
    os.makedirs(lang_dir, exist_ok=True)
    
    clean_text = text.strip()
    assert len(clean_text) <= 500, f"Error: {lang} release note exceeds 500 chars limit ({len(clean_text)})"
    
    # Write whatsnew.txt and whatsnew-8.txt
    with open(os.path.join(lang_dir, "whatsnew.txt"), "w", encoding="utf-8") as f:
        f.write(clean_text)
    with open(os.path.join(lang_dir, "whatsnew-8.txt"), "w", encoding="utf-8") as f:
        f.write(clean_text)
        
    print(f"Generated v2.0.1 release notes for {lang}: length={len(clean_text)} chars (OK <= 500)")

print("\n--- ALL 21 LOCALES GENERATED SUCCESSFULLY ---")
