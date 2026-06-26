# V1 release precheck

## Required Render environment variables

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` (long, unique production value)
- `ALLOWED_ORIGINS` (comma-separated, actual web origins only; leave unset for Android-only releases)
- Firebase, Stripe, mail, and backup variables used by the enabled features

The backend runs Flyway and Hibernate validation at startup. Never edit a
released migration or run `flyway repair` as a workaround for a checksum
mismatch. Restore the released migration and add a new forward-only migration.

## APK hosting with Supabase Storage

1. In the existing Supabase project, create the `app-releases` Storage bucket.
2. Make only that bucket public; do not expose database credentials or a service-role key.
3. Upload the signed APK with an immutable name, such as `mybill-v1.0.0.apk`.
4. Copy its public HTTPS URL, which has this shape:
   `https://<project-ref>.supabase.co/storage/v1/object/public/app-releases/mybill-v1.0.0.apk`.
5. Calculate SHA-256 locally and publish the URL/hash with `POST /api/admin/app-releases`.

## Mandatory device checks

- New install and upgrade from the preceding signed APK.
- Android 12, 13, and 14 installer permission flow.
- Offline invoice creation then sync: verify the server-authoritative invoice
  number replaces the local value before PDF/share is used.
- Advance, balance adjustment, payment, ledger, export, restore, and update
  data-retention flows
