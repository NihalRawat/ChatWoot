# ChatWoot
Chatwoot is an open-source, omnichannel customer support platform designed to be a transparent and self-hostable alternative to tools like Intercom or Zendesk.  It helps a company by acting as a "Central Command Center" for every single customer interaction, regardless of where the conversation starts.

# CHATWOOT =================
Email configuration: below 
Under setting select inbox and add mail 


 # =================================================
smtp.gmail.com
587
STARTTLS
gmail.com (or blank)
app password
# =================================================
imap.gmail.com
993
SSL enabled
app password
=================================================

# Under setting create a automcation rule 



Go on server where chatwoot is hosted through docker or something 
Go to path eg : /opt/chatwoot 
Ls -la 
Open .env file and add below text 
MAILER_SENDER_EMAIL=
SMTP_ADDRESS=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTHENTICATION=login
SMTP_ENABLE_STARTTLS_AUTO=true
SMTP_OPENSSL_VERIFY_MODE=none

And then restart docker by below command first check through chat gpt that data will deleted or remain then perform the further operation’s 

docker compose down
docker compose up -d
This will safely restart Chatwoot with your new .env.
