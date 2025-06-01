Anontalk: Aplicación de Mensajería Anónima y Segura

Anontalk es una aplicación de escritorio centrada en la privacidad del usuario. Su objetivo principal es garantizar que solo el remitente y el destinatario puedan leer los mensajes, utilizando para ello un esquema de cifrado de extremo a extremo basado en criptografía híbrida.

🚀 Características principales

Cifrado híbrido: AES-GCM (256 bits) + RSA-OAEP (2048 bits)

Cliente JavaFX moderno, multilenguaje y con temas claro/oscuro

Backend Spring Boot con APIs REST seguras

Almacenamiento cifrado en PostgreSQL (Render)

Recuperación de contraseña mediante token por email (Spring Mail)

Polling HTTP asíncrono para notificaciones en tiempo real

Exportación de clave pública desde el perfil

Adjuntos cifrados, descifrados localmente

🌐 Tecnologías utilizadas

Java 21 + JavaFX para la interfaz de escritorio

Spring Boot 3.2 + Spring Security + Spring Data JPA

PostgreSQL como base de datos en Render

Jackson, Maven, PBKDF2, AES-GCM, RSA-OAEP

Spring Mail para tokens de recuperación

GitHub para control de versiones

🔒 Seguridad

Todas las claves privadas permanecen cifradas y locales

Ningún mensaje o adjunto se almacena en texto claro

Solo se guarda la metadata mínima (remitente, destinatario, timestamps)

Preparado para operar sobre HTTPS (en producción)

El servidor actúa solo como transportista de bloques cifrados

🏙️ Arquitectura

Cliente ligero en JavaFX (UI reactiva, multitema, i18n)

API REST en Spring Boot para usuarios, mensajes y adjuntos

Polling HTTP cada 5s simula un "chat en vivo"

Base de datos PostgreSQL cifrada (solo almacena Base64)

📆 Estado del proyecto

Versión actual: v1.3.4-dev

Este repositorio contiene la última versión de desarrollo del proyecto. Aunque funcional, algunas mejoras como WebSockets, soporte Tor o chat grupal están previstas para futuras versiones.

🔧 Instalación

Clonar el repositorio:git clone https://github.com/DavidBarbosaOlayo/Anontalk.git

Configurar la base de datos PostgreSQL (en Render u otro proveedor):

Crear la BD y obtener URL JDBC

Establecer las variables de entorno necesarias

Backend (Spring Boot):

Requisitos: Java 21, Maven

Ejecutar: mvn spring-boot:run

Cliente (JavaFX):

Asegurar dependencias JavaFX estén en el classpath

Ejecutar desde tu IDE o crear un .jar ejecutable

📖 Documentación

Manual de Usuario (PDF)

Memoria del Proyecto (PDF)

Diagrama de arquitectura, flujo de cifrado, estructura de carpetas, etc.

🚧 Roadmap



✊ Filosofía del proyecto

Anontalk nace como una reivindicación de la privacidad digital: una herramienta construida desde cero para devolver al usuario el control total sobre sus comunicaciones. Su modelo de "Security by Design" elimina intermediarios de confianza, aplica cifrado robusto en origen y asegura que ningún tercero pueda acceder a los datos.

✉️ Contacto

Autores: David Barbosa Olayo, Marc Mancilla Pontejo

Centro: IES Puig Castellar (DAM2 B)

Curso: 2024–2025

👁️ Licencia

Este proyecto está licenciado bajo Creative Commons Reconocimiento-NoComercial-SinObraDerivada 3.0 España.

“La privacidad no debería ser un lujo, sino el punto de partida.”

