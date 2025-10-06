# SmartLED (Android + Bluetooth)
Login: `admin / 1234`

## Cómo probar
1. Instala en **dos** teléfonos Android.
2. Empareja por Bluetooth en Ajustes (A ↔ B).
3. Abre la app y haz login.
4. En A: “Iniciar como HOST” (esperando conexión).
5. En B: elige A en el spinner → “Conectar a HOST”.
6. Botón “Encender/Apagar LED”: cambia en ambos.

## Tech
Kotlin, Bluetooth RFCOMM (SPP), SQLite, SHA-256, Coroutines.
