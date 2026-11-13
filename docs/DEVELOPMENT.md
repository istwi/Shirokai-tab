# Desenvolvimento no ambiente cloud

O Java pré-instalado nesta instância é JRE 21 e não contém `javac`.
Instalamos Temurin JDK 21.0.13+9 em `/workspace/toolchains/jdk-21.0.13+9`,
a partir da release oficial `adoptium/temurin21-binaries`, verificando o SHA-256
publicado junto ao arquivo antes de extrair.

`tools/gradle_cloud.py` seleciona esse JDK quando JAVA_HOME não está definido,
configura as propriedades de proxy JVM a partir das variáveis herdadas e usa
o truststore Debian `/etc/ssl/certs/java/cacerts`, que contém a CA do ambiente,
mantendo a verificação TLS. O truststore padrão do Temurin não continha essa CA
e causou PKIX antes deste ajuste. Usa cache `/workspace/.gradle-shirokai` por padrão. Não imprime credenciais.

```sh
python3 tools/gradle_cloud.py build
```

Em outra máquina, instale JDK 21 completo, defina JAVA_HOME e execute
`bash ./gradlew --no-daemon build`. A primeira execução requer acesso às
fontes Maven Fabric, Mojang, Maven Central e distribuição Gradle/GitHub.
Um JRE sozinho não permite compilar.

Nenhum script ou mudança de configuração de ambiente foi salvo/publicado
nesta tarefa de desenvolvimento. O JDK e cache atuais são arquivos locais;
não há garantia de restauração em novas tarefas.

0.17.0: JDK21 oficial novamente instalado com SHA-256 verificado nesta instância.
O download privado do Dex usa asset oficial via API GitHub (Accept octet-stream),
com o mesmo SHA-256 fixado; nenhum checksum foi relaxado. Sem mudanças/publicação
de configuração do ambiente.
