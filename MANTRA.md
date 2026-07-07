# Shrink PDF Mantra

Shrink PDF should become the user's go-to emergency document tool.

When someone urgently needs to compress a PDF, convert text to PDF, save a smaller
file, or prepare a document to send, the app should feel dependable, fast, and
obvious.

## Core Promise

Open it when you need the document fixed now.

## Product Philosophy

Shrink PDF is a private, local-first document utility.

The user's files should stay under the user's control. The app should process PDFs
and text files on-device, without uploading documents to a server, requiring an
account, or hiding where output files are saved.

The app should behave like a tool, not a trap. Users open it because they need to
finish a document task quickly.

## Principles

- Local-first: files are processed on the device.
- User control: users choose input files and output locations.
- Original-safe: never overwrite the source file.
- Fast path first: common tasks should be obvious immediately.
- No forced detours: never block file selection, conversion, saving, opening, or
sharing.
- Clear defaults: users should not need to understand compression theory.
- Useful feedback: show progress, success, failure, and file-size results clearly.
- Recoverable errors: explain what happened and what the user can do next.
- Transparent results: show whether compression helped and by how much when known.
- No dark patterns: never make users hunt for close buttons, skip buttons, saved
files, or the original document.
- Preserve focus: monetization and secondary features must stay outside the critical
document workflow.

## Privacy

User documents must not be uploaded for compression or conversion.

The app should not collect, inspect, sell, or transmit document contents. File
access should happen through Android's document picker or user-selected folders. The
app should only access files the user explicitly chooses.

Ad and payment SDKs may process the limited data required for ads or purchases, but
this must be disclosed clearly in the privacy policy.

No account should be required for basic use.

## Ads

Ads are allowed, but they must be quiet.

Ads must never make the user feel blocked, watched, delayed, or tricked. The free
version may show respectful banner ads on idle or result surfaces.

Ads must never appear while a file is being selected, compressed, converted, saved,
opened, shared, or recovered from an error.

## Payment

Premium payment is handled through Google Play Billing.

Premium should be simple:
- one-time purchase preferred
- removes ads
- does not block basic PDF compression
- does not block basic Text-to-PDF conversion
- no confusing credits or subscriptions for v1

The app should never ask users to pay outside the trusted Android and Google Play
purchase flow.

## Decision Test

Every feature should be judged by one question:

Does this help someone finish an urgent document task faster and with more
confidence?

If not, it should wait.