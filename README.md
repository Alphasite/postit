# Post It

CS 5431 Post It Project

To build:
```
postit          > ./gradlew clean
postit          > ./gradlew build
```
To run client:
```
postit          > cd swing/build/distributions
distributions   > unzip swing.zip
distributions   > cd swing
postit          > java -cp 'lib/*:bin' postit.client.GuiApp
```
To run server:
```
postit          > cd swing/build/distributions
distributions   > unzip server.zip
distributions   > cd server
postit          > java -cp 'lib/*:bin' postit.server.ServerApp

If this is your first time running the system in that directory, you will be required to either register or login.
Regardless, you will be prompted for a master password (from which your master key will be derived)
  
From here, there you have to option of creating a keychain, then adding a password (with or without the password generator). Once you have a keychain and a password, you can add or delete keychains and add, edit, or delete passwords.

You can also sync your keychain file with the server copy. Currently, the system uses a scheme that tries to merge the keychains and the passwords. Preference is given to the keychain with the most recent timestamp.
