# emailSender
0. Login your gmail account and go to url https://www.google.com/settings/security/lesssecureapps and turn on “Allow less secure apps”

1. install jre
`$ sudo apt install default-jre`

2. make email_sender.sh executuable
`$ sudo chmod +x email_sender.sh`

3. init EmailSender
`$ java -jar EmailSender.jar -c`

4. update a new "payload.json" file:
"emailAddressFrom" and "emailAddressTo" properties

5. encript your email password
`$ java -jar EmailSender.jar -e`

6. update "catchup" property if needed into email_sender.sh file:
catchup="solana catchup ~/solana/validator-keypair.json --our-localhost"

7. run the email_sender.sh script or create a service
`$ ./email_sender.sh`

In order to compile this app - the following tools are needed:

1) JDK v.8

2) Apache Maven 3.3.9

To assembly via command line, type:

`$ mvn clean package assembly:single`


In order to run the app, go to the `target/` folder and type

`$ java -jar target/emailsender-1.0-SNAPSHOT-jar-with-dependencies.jar`
