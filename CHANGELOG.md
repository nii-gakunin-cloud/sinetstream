# Changelog

<!---
https://keepachangelog.com/
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
--->

## [v1.7.2] - 2022-09-XX

### Security

- Update versions of dependent packages

## [v1.7.1] - 2022-09-XX

- Update versions of dependent packages

## [v1.7.0] - 2022-09-XX

### Added

- Data Compression (Python, Java)
- S3 plugin (Python, Java)
- java/sample/perftool
    - send/receive data and output the obtained metrics information
    - like python/sample/perftool
- python/sample/libs/sinetstream-cmd
    - A simple producer using SINETStream
    - SimpleProducer periodically publishes data on a Raspberry Pi

### Changed

- Support Java 11 (Java 8 is no longer supported)

### Fixed

- Calculate fingerprints correctly in config-server protocol (Python, Java)

## [v1.6.2] - 2022-01-19

### Fixed

- Enter the passphrase of private_key.pem if encrypted.

## [v1.6.1] - 2021-12-22

### Security

- use avro-1.11.0 (Java)

## [v1.6.0] - 2021-12-22

### Added

#### Python, Java, Document

- Support for config-server

#### sinetstream-android (lib)

- Support for getting SSL/TLS certificates from the Android Keystore.
- Support for data encryption/decription.
- Add type-specific Reader/Writer classes extended from generic ones.

#### sinetstream-android-helper (lib)

- Support for getting SSL/TLS certificates from the Android Keystore.
- Support automatic location update for the output JSON data.

#### sinetstream-andorid-echo (app)

- SettingsActivity: SINETStream: Add detailed parameters for MQTT and SSL/TLS.

#### sinetstream-andorid-sensor-publisher (app)

- SettingsActivity: SINETStream: Add detailed parameters for MQTT and SSL/TLS.
- SettingsActivity: Sensor: Add `automatic location update` mode with GPS or FLP.
- MainActivity: Add location tracker and foreground services for GPS and FLP.
- MainActivity: Show location monitor window if location is enabled.

### Changed

#### Common

- build.gradle: Use MavenCentral instead of jCenter
- build.gradle: Use JDK 11 instead of JDK 8, from Android Studio Arctic Fox.

#### sinetstream

- Support Python 3.8 since Python 3.6 will be EoL. (Python)

#### sinetstream-android (lib)

- misc/Dockerfile: Update `openjdk`, `Command line tools` and `SDK Build Tools`.
- API: Split initialization process to 2 phases; initialize() and setup().
- API: User can now abort initialization process if something goes wrong.

#### sinetstream-android-helper (lib)

- misc/Dockerfile: Update `openjdk`, `Command line tools` and `SDK Build Tools`.

#### sinetstream-android-echo (app)

- SettingsActivity: Rearrange menu hierarchy.
- MainActivity: For SSL/TLS connection, operation will be intercepted by a system dialog to pick up credentials.
- MainActivity: Received message will be shown in timeline, instead of the latest only.
- MainActivity: Use typed Reader/Writer classes in `sinetstream-android` library.

#### sinetstream-andorid-sensor-publisher (app)

- SettingsActivity: Rearrange menu hierarchy.
- MainActivity: For SSL/TLS connection, operation will be intercepted by a system dialog to pick up credentials.
- MainActivity: For `automatic location update` mode, operation might be intercepted by several system dialogs to set appropriate permissions.

### Removed

#### sinetstream-android-echo (app)

- SettingsActivity: Exclude TLSv1 and TLSv1.1 from menu items, and set TLSv1.2 as default.

#### sinetstream-android-sensor-publisher (app)

- SettingsActivity: Exclude TLSv1 and TLSv1.1 from menu items, and set TLSv1.2 as default.

### Fixed

#### sinetstream-android (lib)

- Make MQTT connection setup/close in robust way.
- Add missing try-catch clause.
- Resolve NullpointerException cases.
- Resolve some lint warnings.

#### sinetstream-andorid-echo (app)

- MainActivity: Keep some attributes beyond Activity's lifecycle.

#### sinetstream-android-sensor-publisher (app)

- MainActivity: Keep some attributes beyond Activity's lifecycle.
- MainActivity: Fix location notation: (longitude,latitude) -> (latitude,longitude)
- MainActivity: Resolve race conditions between Sensor and Network; bind SensorService after connection has established, and unbind SensorService after connection has closed.

## [v1.5.3] - 2021-05-20

### sinetstream-android-echo

#### Added

- MainActivity: Show receiver fragment contents with history.

#### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.
- MainActivity: Adjust screen layout.
- SettingsActivity: Use fixed service name.

#### Fixed

- MainActivity: Resolve race conditions between modal dialogs.
- MainActivity: Keep the receiver fragment contents, even if the activity
has re-created after suspend/resume.

### sinetstream-android-sensor-publisher

#### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.

#### Fixed

- MainActivity: Resolve race conditions between modal dialogs.

## [v1.5.2] for Android - 2021-05-20

### sinetstream-android

#### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.
- AndroidConfigLoader: Rewrite usage of obsoleted Kotlin functions.

#### Fixed

- CipherXXX: Resolve implementation compatibility issues (work in progress).
- MqttAsyncMessageIO: Now user can abort the ongoing connection request.

### sinetstream-android-helper

#### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.

## [v1.5.2] - 2021-04-23

### Added

- English documents for tutorial-android
- The message format specification
- All-in-one tutorial container supports Raspberry Pi OS
    - tested on Raspberry Pi 4 Model B with mem 4GB
- python/sample/perftool
    - send/receive data and output the obtained metrics information

### Fixed

- No division by zero occurs when calculating an average in a metrics information. (Python, Java)
- A metrics can be gotten after close. (Python, Java)

## [v1.5.1] - 2021-03-24

### Fixed

- docfix: tutorial for Android
- bugfix: 2 sample applications for Android

## [v1.5.0] - 2021-03-18

### Added

- Tutorial for Android
- 2 sample applications for Android
    - sinetstream-android-echo
    - sinetstream-android-sensor-publisher

### Fixed

- Bugfix for data encryption. (Python, Java)
- Bugfix for metrics API (Python)

## [v1.4.0] - 2020-10-08

### Added

- Support Android
    - Limitations:
        - Data encryption is not implemented.

## [v1.3.0] - 2020-07-31

### Added

- Support metrics API

## [v1.2.0] - 2020-06-08

### Added

- Support async API
- Broker setup instructions (Japanese only)

### Changed

- MQTT's MqttClientPersistence directory is now `~/.mqtt-persistence`(Java).

### Fixed

- Fixed TLS connection setup problem, caused by MQTT-specific parameter "tls_set" (Java)

## [v1.1.0] - 2020-03-19

### Added

- The capability to add `timestamp` to each send message
- Support for image type messages.
- English documents
- SINETStream Server Plugin Developer Guide (Japanese only)

### Changed

- Message is encoded by Apache Avro.
- `value_type` can be handled as a plugin.

### Fixed

- Fixed exception handling in case of authentication / authorization error.

## [v1.0.0] - 2019-12-24

### Added

- Tutorial
- open/close method for MessageReader/MessageWriter (Python)

### Changed

- {python,java}/sample/text/*.py: remove -t option to specify the topic.
- Documents rearranged.
- To display the cheat sheet, `python3 -m sinetstream`. (Python)
- The topic= argument is now optional in MessageReader/MessageWriter constructor. (Python)

### Fixed

- Fix documents
- Fix deadlock in MqttWriter.publish().
- Bugfixes.

## [v0.9.7] - 2019-10-11

### Added

- Data encryption:
    - Supported: CBC, OFB, CTR, EAX, GCM
    - Not supported: CFB, OPENPGP
- Authentication:
    - Kafka:
        - security_protocol: PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL.
        - sasl_mechanism: PLAIN, SCRAM-SHA-256, SCRAM-SHA_512.
        - Note: GSSAPI and OAUTHBEARER are not supported.
    - MQTT:
        - password
    - Read docs/auth.md for details.
- Authorization(document only)
    - Read docs/auth.md for details.

## [v0.9.5] - 2019-08-26

### Added

- Java implementation.
- Python/MQTT plugin.
- parameter value_type=.
- display default paraterers during installation.
- TLS support.

### Changed

- default consistency is AT_MOST_ONCE (was EXACTLY_ONCE).
- default client_id is generated by SINETStream library (not Kafka,MQTT).

### Fixed

- many bugfixes.

## [v0.9.1] - 2019-07-11

first alpha release.
