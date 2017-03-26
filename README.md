# Post It

CS 5431 Post It Project

To run:
```
postit          > ./gradlew clean
postit          > ./gradlew build
postit          > cd build/distributions
distributions   > unzip postit.zip
distributions   > cd postit
```

Macs:
```
postit          > java -cp 'lib/*:bin' postit.client.GuiApp
postit          > java -cp 'lib/*:bin' postit.server.ServerApp
```

PC:
```
postit          > java -cp 'lib/*;bin' postit.client.GuiApp
postit          > java -cp 'lib/*;bin' postit.server.ServerApp
```

If this is your first time running the system in that directory, you will be required to either register or login.
Regardless, you will be prompted for a master password (from which your master key will be derived)
  
From here, there you have to option of creating a keychain, then adding a password. Once you have a keychain and a password, you can add or delete keychains and add, edit, or delete passwords.

You can also sync your keychain file with the server copy. Currently, the system uses an overriding scheme. Whichever keychain is the most (your local version or the server's version) recent will override the other.
