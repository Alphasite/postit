# Post It

CS 5431 Post It Project

To run:
                > ./gradlew clean
                > ./gradlew build
                > cd build/distributions
distributions   > unzip postit.zip
distributions   > cd postit

Macs:
postit          > java -cp 'lib/*:bin' postit.client.gui.KeychainViewer


PC:
postit          > java -cp 'lib/*;bin' postit.client.gui.KeychainViewer

You will be prompted for a master password (if this is your first time running the system, you will be setting your master password)
  
From here, there you have to option of creating a keychain, then adding a password. Once you have a keychain and a password, you can add or delete keychains and add, edit, or delete passwords.

When entering the master password, avoid closing the dialog box because the system will crash.
