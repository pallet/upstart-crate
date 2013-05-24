[Repository](https://github.com/pallet/upstart-crate) &#xb7;
[Issues](https://github.com/pallet/upstart-crate/issues) &#xb7;
[API docs](http://palletops.com/upstart-crate/0.8/api) &#xb7;
[Annotated source](http://palletops.com/upstart-crate/0.8/annotated/uberdoc.html) &#xb7;
[Release Notes](https://github.com/pallet/upstart-crate/blob/develop/ReleaseNotes.md)

A pallet crate to install and configure upstart.

### Dependency Information

```clj
:dependencies [[com.palletops/upstart-crate "0.8.0-alpha.2"]]
```

### Releases

<table>
<thead>
  <tr><th>Pallet</th><th>Crate Version</th><th>Repo</th><th>GroupId</th></tr>
</thead>
<tbody>
  <tr>
    <th>0.8.0-beta.6</th>
    <td>0.8.0-alpha.2</td>
    <td>clojars</td>
    <td>com.palletops</td>
    <td><a href='https://github.com/pallet/upstart-crate/blob/0.8.0-alpha.2/ReleaseNotes.md'>Release Notes</a></td>
    <td><a href='https://github.com/pallet/upstart-crate/blob/0.8.0-alpha.2/'>Source</a></td>
  </tr>
  <tr>
    <th>0.7.2</th>
    <td>0.7.0-alpha.2</td>
    <td>clojars</td>
    <td>com.palletops</td>
    <td><a href='https://github.com/pallet/upstart-crate/blob/0.7.0-alpha.2/ReleaseNotes.md'>Release Notes</a></td>
    <td><a href='https://github.com/pallet/upstart-crate/blob/0.7.0-alpha.2/'>Source</a></td>
  </tr>
</tbody>
</table>

## Usage

The `upstart` configuration does not replace the system init as PID 1.

The `server-spec` function provides a convenient pallet server spec for
upstart.  It takes a single map as an argument, specifying configuration
choices, as described below for the `settings` function.  You can use this
in your own group or server specs in the :extends clause.

```clj
(require '[pallet/crate/upstart :as upstart])
(group-spec my-upstart-group
  :extends [(upstart/server-spec {})])
```

While `server-spec` provides an all-in-one function, you can use the individual
plan functions as you see fit.

The `settings` function provides a plan function that should be called in the
`:settings` phase.  The function puts the configuration options into the pallet
session, where they can be found by the other crate functions, or by other
crates wanting to interact with upstart.

The `install` function is responsible for actually installing upstart.

The `configure` function writes the upstart configuration file, using the form
passed to the :config key in the `settings` function.

To create an upstart job, you can write a method for
[`supervisor-config-map`](http://palletops.com/api/0.8/pallet.crate.service.html#var-supervisor-config-map).

```clj
(defmethod supervisor-config-map [:riemann :runit]
  [_ {:keys [run-command service-name user] :as settings} options]
  {:service-name service-name
   :run-file {:content (str "#!/bin/sh\nexec chpst -u " user " " run-command)}})
```

## Support

[On the group](http://groups.google.com/group/pallet-clj), or
[#pallet](http://webchat.freenode.net/?channels=#pallet) on freenode irc.

## License

Licensed under [EPL](http://www.eclipse.org/legal/epl-v10.html)

Copyright 2013 Hugo Duncan.
