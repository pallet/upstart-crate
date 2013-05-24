# Release notes

## upstart-0.8.0-alpha.2

- Make service file verification optional

- Make service actions idempotent
  The :start, :stop, and :restart actions should not complain if the service 
  is not in the expected state.

- Make codox link to correct source


## upstart-0.8.0-alpha.1

- Update versions for pallet 0.8.0-beta.6

- Rename jobs to configure
  Standardising on the configure name for the plan function that writes 
  configuration files.

- Make :start succeed if already running
  A process :start action should succeed if the process is already running. 
  The upstart start command returns failure in this situation, so this adds
  an explicit check for this state.

- remove the quoted-string keys from list of keys with :simple formatting

- Update for service abstraction

- Use lein as build tool

## upstart-0.7.0-alpha.1

- Add a defmethod for quoted config strings

- Add support for the :setuid stanza, and default unknown keywords to :simple

- Fix names var to not be a function

## pallet-crates-0.5.0


## pallet-crates-0.4.4


## pallet-crates-0.4.3

- Update for 0.5.0-SNAPSHOT
  Change pallet.resource.* to pallet.action.*. Change stevedore calls to
  script functions to use unquote and the pallet.script.lib namespace. 
  Change request to session.  Change build-resources to build-actions.


## pallet-crates-0.4.2


## pallet-crates-0.4.1


## pallet-crates-0.4.0


## pallet-crates-0.4.0-beta-1

- working node-list compute service

- added upstart crate, and parameters/assoc-for-target, assoc-for-service,
  etc

- Add upstart (and comment using it to start a node.js server)
