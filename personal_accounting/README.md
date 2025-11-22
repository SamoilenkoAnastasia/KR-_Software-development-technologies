JavaFX Personal Accounting (Desktop) — Prototype

How to run:
1. Ensure MySQL server is running and create database:
   CREATE DATABASE personal_accounting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

2. Adjust DB credentials in src/main/resources/config.properties if needed.
   Currently:
   db.url=jdbc:mysql://127.0.0.1:3306/personal_accounting?useSSL=false&serverTimezone=UTC
   db.user=root
   db.password=N_030306-a

3. Build & run with Maven:
   mvn clean package
   mvn javafx:run

Or run the shaded jar:
   java -jar target/personal-accounting-javafx-0.0.1-SNAPSHOT.jar

Notes:
- This prototype uses plain JDBC for simplicity and clarity.
- Passwords are stored in plaintext for the prototype; in production, hash them.
- Implemented features: register/login, categories, accounts, add/list transactions, account balances update.
- UI uses JavaFX FXML. Open project in IntelliJ/NetBeans and run.

