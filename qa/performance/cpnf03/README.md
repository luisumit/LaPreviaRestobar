# CPNF-03: carga concurrente local

`CPNF-03-15-usuarios.jmx` crea 15 pedidos sintéticos en Firebase Realtime Database Emulator. No apunta a Firebase de producción ni mide, por sí solo, cuánto tarda cada pedido en aparecer en la pantalla de cocina.

## Ejecución

1. Inicia los emuladores locales desde la raíz del proyecto:

   ```powershell
   firebase emulators:start --only auth,database --project laprevia-restobar
   ```

2. Abre el plan JMX con Apache JMeter 5.6.3 y ejecútalo en modo gráfico, o usa:

   ```powershell
   jmeter -n -t qa/performance/cpnf03/CPNF-03-15-usuarios.jmx -l resultados.jtl
   ```

3. Revisa el resumen de JMeter: número de solicitudes, errores y tiempos de escritura.

El plan usa 15 hilos, una escritura por hilo, inicio simultáneo y una aserción de 500 ms para cada escritura en el emulador. Los registros tienen la nota `CPNF-03 solo para emulador local` para distinguirlos de pedidos reales.

## Alcance de la medición

El tiempo de respuesta de JMeter corresponde a la escritura HTTP en el emulador. Para evaluar el tiempo extremo a extremo hasta la pantalla de cocina hace falta instrumentar y medir la recepción en la app ejecutada contra ese mismo emulador. No se debe inferir un resultado PASS/FAIL de llegada a cocina usando únicamente el `.jtl`.

La configuración local y los resultados generados (`.jtl`, logs, capturas e informes HTML) no se versionan; conserva esos artefactos aparte como evidencia de la ejecución concreta. Una prueba contra el emulador sirve para verificar el plan y no representa el rendimiento de producción.
