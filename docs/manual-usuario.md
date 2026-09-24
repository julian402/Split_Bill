# SplitBill — Manual de usuario

SplitBill sirve para llevar las cuentas de un grupo: el paseo, la casa, la oficina. Anotas quién pagó
cada cosa y la app te dice cuánto debe cada uno y cuál es la **menor cantidad de transferencias** para
quedar a paz y salvo.

La app funciona **sin internet**: todo se guarda en el celular y se sube solo cuando vuelve la conexión.

---

## 1. Entrar

- **Crear cuenta**: nombre, email, teléfono (opcional) y una contraseña de al menos 8 caracteres.
- **Iniciar sesión**: email y contraseña. La sesión dura 24 horas; si vence, la app te devuelve aquí
  sin perder lo que habías anotado.

Si ya habías usado SplitBill sin cuenta en ese celular, tus gastos se suben a tu cuenta al entrar.
Después la app te pregunta si estabas en la lista de integrantes, para que no quedes repetido (ver
"¿Ya estabas en la lista?").

## 2. Pantalla principal

- Arriba está el **nombre del grupo**. Tócalo para cambiar de grupo o crear uno nuevo.
- Debajo, el estado de la sincronización: *Sincronizado*, *Sin conexión · 2 cambios pendientes*…
  - El botón ⟳ sincroniza a mano.
  - El botón de salida cierra la sesión. Si hay cambios sin subir, la app te avisa antes, porque al
    cerrar sesión se borran los datos del celular.
- La tarjeta muestra el **total del grupo**, con los accesos a **Liquidar** y **Cuenta rápida**.
- La lista muestra los gastos, del más reciente al más antiguo.
  - Toca un gasto para ver el detalle.
  - La papelera lo borra, después de confirmar.
- **Agregar gasto** abre el formulario. Si el grupo tiene menos de dos integrantes, primero te lleva a
  agregarlos.

## 3. Grupos

- Cada grupo tiene sus propios integrantes, gastos y liquidación.
- **Nuevo grupo**: le das un nombre (por ejemplo "Viaje a Cartagena"), quedas dentro de él y la app te
  lleva a agregar a sus integrantes.
- Toca un grupo para entrar en él.
- Mantén presionado un grupo para **cambiarle el nombre**. Solo lo puede cambiar quien lo creó.

## 4. Integrantes

- Escribe el nombre (y el teléfono, si quieres) y toca **Guardar**.
- **Agregar desde contactos**:
  1. La primera vez, Android te pide permiso para leer tus contactos. SplitBill solo los usa para esta
     lista y no sube tu agenda.
  2. Busca por nombre o teléfono y marca a varios a la vez.
  3. Toca **Agregar**. Los que ya están en el grupo salen marcados en gris.
- **¿Ya estabas en la lista?** Si alguien te había agregado por nombre antes de que tuvieras cuenta,
  busca tu nombre y confirma. Tus gastos pasan a tu cuenta y quedas una sola vez.
- La papelera retira a un integrante. Sus gastos anteriores se conservan y siguen contando en los
  saldos.

Si el permiso de contactos se negó dos veces, Android ya no lo vuelve a preguntar. En ese caso la app
te ofrece **Abrir ajustes** para activarlo a mano.

## 5. Registrar un gasto

1. **Descripción** y **monto**. El ícono de cámara del monto abre el escáner de facturas.
2. **Quién pagó**.
3. **Cómo se divide**:
   - **Partes iguales** entre los participantes marcados.
   - **Montos exactos**: escribes cuánto le toca a cada uno. Deben sumar el total.
   - **Porcentajes**: deben sumar 100 %.
4. Marca los **participantes** y toca **Guardar gasto**.

Si sales con cambios sin guardar, la app te pregunta antes de descartarlos.

**Detalle del gasto**: quién pagó, cuándo, cuánto le toca a cada uno (y su porcentaje), y si ya se
subió al servidor. Desde aquí puedes **Editar** o **Eliminar** el gasto.

## 6. Escanear una factura

1. Toca el ícono de **cámara** en el monto, en *Agregar gasto* o en *Cuenta rápida*.
2. La primera vez, Android te pide permiso para usar la cámara. Si no lo das, igual puedes usar
   **Galería**.
3. Encuadra la factura completa, con buena luz, y toca **Tomar foto**. También puedes tocar **Galería**
   y elegir una foto que ya tengas.
4. La app muestra el **total que encontró** y el nombre del comercio. Si no es el total correcto, toca
   otro de los valores de la factura.
5. **Usar este valor** llena el monto. Si la descripción estaba vacía, también la llena con el nombre
   del comercio.

La foto se lee en el celular y no se guarda. Revisa siempre el valor antes de guardar.

## 7. Liquidar

La pantalla **Liquidar** muestra:
- El **saldo** de cada integrante: en verde a quién le deben, en rojo quién debe.
- El **plan de pagos**: quién le paga a quién y cuánto. Por ejemplo, "3 transferencias en lugar de 9".

Si todos están en cero, el grupo está a paz y salvo.

## 8. Cuenta rápida

Para dividir una cuenta en el momento, sin registrar integrantes: la cena, el taxi.

1. Escribe el total, o escanéalo con la cámara, y el % de propina.
2. Con **−** y **+** ajusta cuántas personas son. Puedes ponerles nombre.
3. Escoge cómo se divide y toca **Calcular**.
4. Si quieres que quede en el grupo, **Guardar como gasto** abre el formulario con el total (propina
   incluida) y la descripción ya llenos.

## 9. Sin conexión

- Todo funciona igual: agregar, editar, borrar, crear grupos.
- Los cambios quedan "por subir" y se envían solos cuando vuelve la red, **aunque hayas cerrado la
  app**.
- Si otro integrante cambió algo, lo ves al sincronizar.
- Si el servidor rechaza un cambio (por ejemplo, un gasto que alguien más ya borró), la app te lo
  avisa y deja la versión del servidor.
