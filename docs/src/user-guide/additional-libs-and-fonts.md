# Additional libraries and fonts

GeoServer Cloud containers load user-provided jar files and fonts from two
mount points, without rebuilding the images:

- `/opt/additional_libs`: jar files added to the application classpath.
- `/opt/additional_fonts`: font files registered with the operating system.

Typical uses:

- JDBC drivers that cannot be redistributed, like the Oracle `ojdbc` driver.
- GeoServer extensions not bundled in the official images.
- Corporate or symbol fonts for map labeling with SLD styles.

## Provide additional jar libraries

Mount a directory with the jar files at `/opt/additional_libs` on every
service that needs them.

With `docker run`:

```shell
docker run -v ./my-libs:/opt/additional_libs geoservercloud/geoserver-cloud-wms
```

With Docker Compose:

```yaml
services:
  wms:
    image: geoservercloud/geoserver-cloud-wms
    volumes:
      - ./my-libs:/opt/additional_libs
```

With Kubernetes, use any volume type and mount it at `/opt/additional_libs`:

```yaml
        volumeMounts:
          - name: additional-libs
            mountPath: /opt/additional_libs
```

The jars become part of the application classpath at startup, as peers of the
bundled libraries. They can both provide self-contained libraries like JDBC
drivers and GeoServer extensions that depend on GeoServer and GeoTools
classes.

Note that GeoServer Cloud activates extensions through Spring Boot
autoconfigurations, not through the traditional GeoServer mechanism of
scanning every jar for an `applicationContext.xml` bean definitions file. A
vanilla GeoServer extension jar dropped in `/opt/additional_libs` will not
activate by itself: extensions must be packaged as described in
[Adding Extensions to GeoServer Cloud](../developer-guide/extensions/adding_extensions.md).

### How it works

The containers start the application through Spring Boot's
`PropertiesLauncher`, which reads the `LOADER_PATH` environment variable. The
images set `LOADER_PATH=/opt/additional_libs` by default. To load jars from
several directories, override it with a comma-separated list that includes
the default:

```yaml
    environment:
      LOADER_PATH: /opt/additional_libs,/opt/my-other-libs
```

### Classpath precedence

Jars in `/opt/additional_libs` come before the bundled libraries on the
classpath. This allows replacing the version of a bundled library, but also
means a mounted jar can unintentionally shadow classes the services depend
on. Mount only what you need.

### File permissions

The services run with the user id given by the deployment (for example
`user: ${GS_USER}` in the Docker Compose files). Read access is all the
services need: make sure the mounted files are readable by that uid, or
world-readable. The directory baked into the image is world-writable with the
sticky bit set (`1777`), meaning init containers or `docker cp` can populate
a named volume under any uid.

## Provide additional fonts

Mount a directory with font files (`.ttf`, `.otf`, and any other
fontconfig-supported format) at `/opt/additional_fonts` on the GeoServer
services that render maps or list fonts (at least `wms` and `webui`):

```yaml
services:
  wms:
    image: geoservercloud/geoserver-cloud-wms
    volumes:
      - ./my-fonts:/opt/additional_fonts
```

The directory is registered with fontconfig inside the image, meaning the JVM
finds the fonts like any other system font: they are available to SLD
`<Font>` elements in the `wms` service and listed in the web UI under
*About & Status > Server Status > Fonts*. No startup script copies files
around and no font cache needs rebuilding: fontconfig scans the directory
when the JVM first enumerates fonts.

Fonts referenced by styles must be mounted on every service that renders with
those styles. Subdirectories are scanned recursively, and the same read
access rules as for `/opt/additional_libs` apply.

## Verify

To check a mounted jar is on the classpath, start the service with
`JAVA_OPTS=-Dloader.debug=true`: the launcher prints the resolved classpath
URLs, including the jars found in `/opt/additional_libs`, before the
application starts.

To check the fonts are registered, list them with fontconfig from inside the
container:

```shell
docker compose exec wms fc-list | grep -i myfont
```

or open *About & Status > Server Status > Fonts* in the web UI.
