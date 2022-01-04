#!/bin/bash
catchup="solana catchup ~/solana/validator-keypair.json --our-localhost"
sendemail="java -jar EmailSender.jar -s"

while :
do
    result=$($catchup)
    if [[ $result != *"has caught up"* ]]; then
        $sendemail
    fi
    sleep 1h
done
