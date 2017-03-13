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
