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

## 2. La barra de abajo

Siempre a mano, en la parte de abajo:

| | |
|---|---|
| **Inicio** | El resumen de todos tus grupos |
| **Grupos** | La lista de tus grupos |
| **+** (el botón del centro) | Abre un menú con dos opciones: **Gasto** (en el grupo que tienes abierto) y **Cuenta rápida**. Toca fuera del menú o la **X** para cerrarlo |
| **Actividad** | Todo lo que ha pasado, en todos los grupos |
| **Perfil** | Tus datos, la sincronización y cerrar sesión |

## 3. Inicio

- Arriba, tu saludo y el estado de la sincronización. La **campana** lleva a la actividad; si tiene un
  punto rojo, hay cambios que todavía no se han subido.
- La tarjeta morada suma **todos tus grupos**:
  - **Total gastado** y cuántos grupos y gastos tienes.
  - **Este mes**: lo gastado desde el día 1.
  - **Tu parte**: lo que te tocó a ti en esos gastos.
  - **Tu balance**: en verde, lo que te deben; en rojo, lo que debes.
- **Tus grupos**: cada tarjeta muestra quiénes están y tu saldo en ese grupo (**Te deben**, **Debes** o
  **Todos al día**). Tócala para entrar al grupo; los tres puntos abren sus integrantes y su nombre.
- **Actividad reciente**: los últimos gastos de todos los grupos. Toca uno para ver su detalle.

## 4. Un grupo

- Arriba está el **nombre del grupo**; tócalo para ir a la lista de grupos. Los tres puntos abren sus
  integrantes.
- La tarjeta muestra el **total del grupo** y **tu saldo** en él, con los accesos a **Liquidar** e
  **Integrantes**.
- **Movimientos**: los gastos y pagos, del más reciente al más antiguo, cada uno con el ícono de su
  categoría. Toca uno para ver el detalle.

## 5. Nuevo grupo e integrantes

**Nuevo grupo** (pestaña *Grupos* → *Nuevo grupo*):

1. Escribe el **nombre del grupo** (por ejemplo "Viaje a Cartagena").
2. Agrega a la gente:
   - **A mano**: nombre y teléfono (opcional) → **Agregar integrante**.
   - **Importar contactos**: la primera vez, Android te pide permiso para leer tus contactos. SplitBill
     solo los usa para esta lista y no sube tu agenda. Busca por nombre o teléfono, marca a varios y
     toca **Agregar**.
3. Tú apareces como **Administrador**; los demás, como **Integrante**. La papelera quita a alguien de
   la lista. Con dos o más personas verás "Listo para empezar".
4. **Guardar grupo**: se crea con toda su gente y entras en él.

**Integrantes de un grupo que ya existe** (botón *Integrantes* del grupo): la misma pantalla, pero cada
persona que agregas o quitas se guarda de una vez. Aquí también cambias el **nombre del grupo** (solo
quien lo creó) y está **¿Ya estabas en la lista?**: si alguien te había agregado por nombre antes de
que tuvieras cuenta, busca tu nombre y confirma. Tus gastos pasan a tu cuenta y quedas una sola vez.

Retirar a un integrante no borra sus gastos anteriores: siguen contando en los saldos.

Si el permiso de contactos se negó dos veces, Android ya no lo vuelve a preguntar. En ese caso la app
te ofrece **Abrir ajustes** para activarlo a mano.

## 6. Registrar un gasto

Toca **+** en la barra de abajo y escoge **Gasto**.

1. **Monto total**. El botón de cámara de la derecha abre el escáner de facturas.
2. **Descripción** (la X la borra).
3. **Quién pagó** y **cómo se divide**:
   - **Partes iguales** entre los participantes marcados.
   - **Montos exactos**: escribes cuánto le toca a cada uno. Deben sumar el total.
   - **Porcentajes**: deben sumar 100 %.
4. **Participantes**: toca cada tarjeta para marcarla o desmarcarla. **Seleccionar todos** marca (o
   desmarca) a todos.
5. **Fecha** (hoy, por defecto; no se puede escoger una fecha futura) y **Categoría**: comida,
   mercado, transporte, hospedaje, entretenimiento, servicios u otro.
6. La tarjeta **División estimada** te muestra, mientras escribes, cuánto paga cada uno (o cuánto
   llevas asignado con montos o porcentajes).
7. **Guardar gasto**.

Si el grupo tiene menos de dos integrantes, la app te lleva primero a agregarlos, y desde ahí sigues
con el gasto. Si sales con cambios sin guardar, la app te pregunta antes de descartarlos.

**Detalle del gasto**: categoría, quién pagó, cuándo, cuánto le toca a cada uno (y su porcentaje), y si
ya se subió al servidor. Desde aquí puedes **Editar** o **Eliminar** el gasto.

## 7. Escanear una factura

1. Toca el botón de **cámara** del monto, en *Registrar un gasto* o en *Cuenta rápida*.
2. La primera vez, Android te pide permiso para usar la cámara. Si no lo das, igual puedes usar
   **Galería**.
3. Encuadra la factura completa, con buena luz, y toca **Tomar foto**. También puedes tocar **Galería**
   y elegir una foto que ya tengas.
4. La app muestra el **total que encontró** y el nombre del comercio. Si no es el total correcto, toca
   otro de los valores de la factura.
5. **Usar este valor** llena el monto. Si la descripción estaba vacía, también la llena con el nombre
   del comercio.

La foto se lee en el celular y no se guarda. Revisa siempre el valor antes de guardar.

## 8. Liquidar

Desde un grupo, toca **Liquidar**:

- **Plan de pagos**: por ejemplo, "1 transferencia en lugar de 6".
- **Transferencias sugeridas**: quién le paga a quién y cuánto. En cada una:
  - **Compartir** manda el mensaje por WhatsApp, correo o la app que escojas.
  - **Pagado** registra que esa transferencia ya se hizo.
- **Saldo de cada integrante**: **Le deben** (verde), **Debe** (rojo) o **Saldo en cero**, y cuántas
  transferencias recibe o hace.
- **Resumen**: el total del grupo, cuántas personas ya están saldadas y cuántas transferencias faltan.
- **Marcar como pagado** (abajo) registra todas las transferencias de una vez.

Un pago queda en los movimientos del grupo como "Luis le pagó a Andrés", con la etiqueta **Pago**. No
cuenta como gasto en los totales, pero sí en los saldos: por eso quien pagó queda en cero. Si lo
marcaste por error, ábrelo y toca **Eliminar**.

Si todos están en cero, el grupo está a paz y salvo.

## 9. Actividad

Todos los gastos y pagos de **todos** tus grupos, agrupados por día ("Hoy", "Ayer", "22 de sept.").
Cada fila dice quién pagó y en qué grupo. Toca uno para ver su detalle. Arriba ves el estado de la
sincronización, y el botón ⟳ sincroniza a mano.

## 10. Perfil

- Tu nombre, tu email y tu teléfono. Cambia el nombre o el teléfono y toca **Guardar cambios**: los
  demás integrantes verán tu nombre nuevo. Para esto necesitas conexión.
- **Sincronización**: el estado actual y el botón **Sincronizar**.
- **Cerrar sesión**. Si hay cambios sin subir, la app te avisa antes, porque al cerrar sesión se borran
  los datos del celular.

## 11. Cuenta rápida

Para dividir una cuenta en el momento, sin registrar integrantes: la cena, el taxi. Se abre con **+** →
**Cuenta rápida**.

1. Escribe el total, o escanéalo con la cámara, y el % de propina.
2. Con **−** y **+** ajusta cuántas personas son. Puedes ponerles nombre.
3. Escoge cómo se divide y toca **Calcular**.
4. Si quieres que quede en el grupo, **Guardar como gasto del grupo** abre el formulario con el total
   (propina incluida) y la descripción ya llenos.

## 12. Sin conexión

- Todo funciona igual: agregar, editar, borrar, crear grupos, marcar pagos.
- Los cambios quedan "por subir" (la campana del inicio muestra un punto rojo) y se envían solos cuando
  vuelve la red, **aunque hayas cerrado la app**.
- Si otro integrante cambió algo, lo ves al sincronizar.
- Si el servidor rechaza un cambio (por ejemplo, un gasto que alguien más ya borró), la app te lo
  avisa y deja la versión del servidor.
- Lo único que necesita conexión: cambiar tu perfil y "¿Ya estabas en la lista?".
