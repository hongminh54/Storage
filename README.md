# Maven Repository

This branch contains the Maven repository for the Storage plugin.

## Structure

```
net/
  danh/
    Storage/
      {version}/
        Storage-{version}.jar
        Storage-{version}.pom
        Storage-{version}.jar.md5
        Storage-{version}.jar.sha1
        Storage-{version}.pom.md5
        Storage-{version}.pom.sha1
      maven-metadata.xml
      maven-metadata.xml.md5
      maven-metadata.xml.sha1
```

## Usage

Add this repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>storage-repo</id>
        <url>https://raw.githubusercontent.com/hongminh54/Storage/maven-repo/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.danh</groupId>
        <artifactId>Storage</artifactId>
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Automated Deployment

Artifacts are automatically deployed via GitHub Actions on every push to master.

See `.github/workflows/maven-deploy.yml` for details.
