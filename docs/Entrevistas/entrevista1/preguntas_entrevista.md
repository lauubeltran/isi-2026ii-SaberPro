# Preguntas y respuestas de la entrevista

**Proyecto:** Pruebas Saber Pro — validación del prototipo hasta el requerimiento 4
**Fecha:** 23 de octubre de 2026
**Participantes:** Desarrolladores: Derlys Xiomara Leal Castillo 160005345, Yurley Ximena Lozano Ruiz 160005218, Laura Geraldine Beltran Cediel 160005247, Looana Marinella Leon Guevara 160005346
                   Cliente: Carlos Daniel Gonsález Torres

Las preguntas siguen el orden de la presentación (`presentacion.pdf`) y recogen únicamente los temas que el cliente respondió o completó en la reunión. Los cambios remiten a `cambios_identificados.md`.

**Estados:** *Confirmado* (el cliente lo afirmó), *Solicitado* (el cliente pidió un cambio), *Propuesta* (idea planteada, sin cierre), *Por confirmar* (respuesta incompleta o con dos versiones).

---

## 1. Lista de estudiantes y revisión

**P1. ¿Cual seria la forma mas adecuada de obtener la lista de estudinates?**
- **Respuesta:** Prefiero que se obtenga directamente del SIAU. El cliente cuestiona sacar un archivo y volver a cargarlo
- **Estado:** Solicitado

**P2. ¿El manejo de observaciones a la hora de cambiar lo que sugiere el sistema para admitir un estudiante es adecuada?**
- **Pregunta del cliente:** «¿como manejan ese tema de las observaciones?»
- **Respuesta del equipo:** con «guardar y siguiente» se pasa al próximo estudiante; si el coordinador cambia lo que sugiere el sistema, debe dejar una observación. Se repite hasta completar la lista. El cliente no objetó.
- **Estado:** Confirmado

## 2. Requisitos de habilitación

**P3. ¿El paz y salvo es condición para presentar la prueba?**
- **Contexto:** el equipo lo había incluido porque algunos estudiantes tienen una deuda o un equipo pendiente en Bienestar.
- **Respuesta:** no. El cliente indica que no puede exigirlo, que tienen derecho a presentar aunque tengan la multa.
- **Estado:** Solicitado.
- **Por confirmar:** si el paz y salvo se muestra solo como información o se retira.

**P4. ¿Las notas pendientes deben impedir la habilitación?**
- **Respuesta:** no. Las inscripciones se hacen en noviembre para el año siguiente y casi todos tienen notas pendientes. Además, el porcentaje de créditos ya se calcula con nota: si no hay nota, el crédito no cuenta como aprobado. El equipo concluye que hay que eliminar el requisito.
- **Estado:** Solicitado

**P5. ¿Qué porcentaje de créditos se exige?**
- **Respuesta:** el avance de créditos se mantiene. El cliente dice: «es el 70 %; bueno». También indica que Admisiones solo envía a quienes tienen más del 70 % de créditos.
- **Estado:** Por confirmar · **Obs.:** se interpreta que 70 % es el umbral con el que Admisiones remite estudiantes y 75 % el del programa; falta confirmarlo. 

## 3. Roles

**P6. ¿El coordinador realiza el preregistro y envía las notificaciones?**
- **Respuesta:** el coordinador: «todo lo hace el coordinador». Prácticamente todo su trabajo es notificar y preregistrar.
- **Estado:** Confirmado

**P7. ¿Qué papel queda para el director?**
- **Respuesta:** el cliente señala que la vista que mostraba el prototipo (envío de la lista) correspondía al director y que ahora todo recae en el coordinador. El equipo reconoce que el modelo sobrecargaba al coordinador y que hay que rediseñarlo; el ajuste de roles se revisará con la docente Olga, con quien se habían levantado requisitos previos.
- **Estado:** Por confirmar (si el director conserva un aval) 

**P8. ¿Cómo recibe el coordinador la lista?**
- **Respuesta del cliente:** la lista se envía por el mismo programa; el coordinador entra con su perfil y la lista aparece allí.
- **Estado:** Confirmado como comportamiento del prototipo 

## 4. Preregistro

**P9. ¿Cómo se completa el proceso después del preregistro?**
- **Respuesta:** son dos fases: la universidad preregistra y luego se envía la información al estudiante para que termine la inscripción. 
- **Estado:** Confirmado 

**P10. ¿El preregistro se hace a toda la lista a la vez o de a uno?**
- **Pregunta del cliente:** «¿Y no lo puedo hacer de a uno?»
- **Respuesta:** debe poder hacerse de a uno o de a varios. El equipo propone un espacio de selección con la opción de marcar todos; al confirmar, los estudiantes quedan preregistrados y notificados.
- **Estado:** Solicitado

**P11. ¿Pueden llegar estudiantes después de enviada la lista?**
- **Respuesta:** sí. Primero envían a los habilitados y después llegan otros que tenían un problema de matrícula ya resuelto.
- **Estado:** Solicitado 

**P12. ¿De cuánto tiempo se dispone para hacer el preregistro?**
- **Respuesta:** el cliente indica que se tiene «hasta quince días».
- **Estado:** Por confirmar (a qué plazo se refiere exactamente)

## 5. Estudiantes que ya presentaron

**P13. ¿Cómo se sabe que un estudiante ya presentó la prueba?**
- **Respuesta:** Admisiones envía a todos los que superan el 70 % de créditos y no sabe quién presentó; eso solo se conoce en el trámite de grado. Se puede presentar desde, aproximadamente, el séptimo semestre, y sin un registro no hay cómo descartar a quien ya presentó. El cliente propone mostrar «presentó / no presentó» en lugar de «paz y salvo». El equipo propone añadir esa validación.
- **Estado:** Propuesta (falta definir de dónde sale el dato y quién lo actualiza) 

## 6. Sección del estudiante

**P14 . ¿Qué debe ofrecer la sección del estudiante?**
- **Respuesta:** el equipo propone un espacio donde llegue el aviso de que debe registrarse, donde pueda inscribir simulacros y talleres, y donde vea las fechas de citación, certificado y prueba, modificables. El cliente añade que la notificación también se envía al correo.
- **Estado:** Propuesta 

## 7. Fechas y notificaciones

**P15. ¿Qué fechas debe gestionar el coordinador y qué se notifica en cada una?**
- **Respuesta:** la prueba (recordatorio de asistencia), la publicación de citaciones (correo que indica dónde presenta cada estudiante), los certificados (ahora se da un plazo y se envían al correo), los resultados y el simulacro (previo, virtual, con permiso académico). En el portal están la publicación de citaciones, la aplicación de pruebas, los certificados y los resultados.
- **Estado:** Solicitado 

**P18. ¿Hace falta poder renotificar?**
- **Respuesta:** sí. Hoy quedan notificados, pero no hay opción de renotificar, y después los estudiantes dicen «no me notificaron» y no presentan. Es especialmente necesario para la fecha de presentación, y hay varias fechas.
- **Estado:** Solicitado 

**P19. ¿Quién debe cargar y modificar las fechas?**
- **Respuesta:** el coordinador. El equipo propone que la notificación salga de acuerdo con las fechas que él tenga cargadas.
- **Estado:** Confirmado 

**P20. ¿Dónde se publican las fechas oficiales?**
- **Pregunta del equipo:** «¿Esas fechas las podemos encontrar en algún sitio?»
- **Respuesta:** no las comparten 
- **Estado:** Confirmado; por eso el coordinador debe poder cargarlas

**P21. ¿Qué debe pasar cuando una fecha cambia?**
- **Respuesta:** el cliente cuenta que la fecha de la prueba se modificó por el terremoto y quedó para el domingo 25 de octubre. El equipo propone avisar a los estudiantes cuando cambie una fecha, llevar un registro de esos cambios y poder notificar en cualquier momento (esto último, en una fase posterior).
- **Estado:** Solicitado 

## 8. Simulacros y cierre

**P22. ¿Los simulacros también serían en línea?**
- **Respuesta:** se hacen en Moodle: los estudiantes se conectan, ven las preguntas y las resuelven. También podrían ser presenciales, según lo que decida el programa. El coordinador gestiona el permiso académico del día del simulacro.
- **Estado:** Confirmado; fuera del prototipo por ahora 

**P23. ¿Con esto termina el flujo de notificaciones?**
- **Respuesta:** «Sí.»
- **Estado:** Confirmado
