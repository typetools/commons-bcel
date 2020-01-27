This is a version of BCEL that is annotated with type annotations for the Checker Framework.


To build this project
---------------------

```
mvn -B -Dmaven.test.skip=true package
```

There will be pluggable type-checking errors, because only the signatures, not
the bodies, of methods are annotated.

This creates file
`target/bcel-VERSION.jar`.


To update to a newer version of the upstream library
----------------------------------------------------

At https://github.com/apache/commons-bcel/tags ,
find the tag corresponding to a public release.

Update the following line in this file, so that others know the current status:
BCEL version 6.4.1 is tag rel/commons-bcel-6.4.1

Pull in that tag:
```
git pull https://github.com/apache/commons-bcel <tag>
```

(Alternately, pull from a repo that contains bug fixes,
such as https://github.com/codespecs/commons-bcel .)

Use the upstream version number as the version number.
That is, if there is a merge conflict related to version numbers,
use the upstream version.

Change `pom.xml` to use the latest Checker Framework version.
(If possible; Error Prone may conflict with the Checker Framework.)

Build the project (instructions appear above).

Test it in a branch of Daikon:
 * copy the bcel-VERSION.jar file to the `daikon/java/lib` directory
 * remove the old bcel-OLDVERSION.jar file
 * run:  make -C java typecheck
   If there are any warnings or errors, then either add annotations
   to BCEL or fix bugs in Daikon.
 * push your branch, and ensure that the the Azure Pipelines tests pass
 * merge your branch into master

Upload BCEL to Maven Central (instructions appear below).


To make changes between upstream releases
-----------------------------------------

If you need to release a new version of BCEL between upstream releases (for
example, because of a bug fix or because of added annotations), use the
version number policy explained at section "Version numbers for annotated
libraries" in
https://rawgit.com/typetools/checker-framework/master/docs/developer/developer-manual.html#annotated-library-version-numbers
.


To upload to Maven Central
--------------------------

This must be done on a CSE machine, which has access to the necessary passwords.

# Set the version number:
#  * in file cfMavenCentral.xml
#  * in file pom.xml (if different from upstream)
#  * environment variable PACKAGE below

PACKAGE=bcel-6.4.1 && \
mvn clean verify && \
mvn javadoc:javadoc && (cd target/site/apidocs && jar -cf ${PACKAGE}-javadoc.jar org)

## This does not seem to work for me:
# -Dhomedir=/projects/swlab1/checker-framework/hosting-info

mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}.jar \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}-sources.jar -Dclassifier=sources \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/site/apidocs/${PACKAGE}-javadoc.jar -Dclassifier=javadoc

# Complete the release at https://oss.sonatype.org/#stagingRepositories :
#  * Search for a repository named orgcheckerframework-NNNN (NNNN are digits)
#  * Click on it
#  * Click "close" at the top.
#  * Click "refresh" at the top until the bottom pane has "Repositery closed"
#  * Click "release" at the top (make sure the "automatically drop" box is checked)
