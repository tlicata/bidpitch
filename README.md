# Bid Pitch

An implementation of the [Bid Pitch][pitch] card game. The goal is to
allow me and my family to play online while we're scattered across the
country.  Based on [Lockport, NY][lockport] local rules.

Another goal of this project is to make use of interesting
technologies. The server is written in [Clojure][clj] and the client
is written in [ClojureScript][cljs]. Game logic is shared between them
using [cljc][cljc].

Websockets handle most of the client-server communication.
[Http-kit][httpkit] provides good websocket implementation and James
Henderson's [Chord][chord] provides a nice [core.async][async]
wrapper. I'm not sure if it was necessary but it was very convenient.

On the front-end I experimented with Facebook's [React.js][react]. I initially
used [Om][om] as a ClojureScript interface, but that is no longer being actively
maintained, so I moved to [Reagent][reagent]. The [Hiccup][hiccup] syntax
definitely feels like an improvement.

[pitch]: http://en.wikipedia.org/wiki/Pitch_(card_game)
[lockport]: http://en.wikipedia.org/wiki/Lockport_(city),_New_York
[clj]: http://clojure.org/
[cljs]: https://github.com/clojure/clojurescript
[cljc]: https://github.com/clojure/clojurescript/wiki/Using-cljc
[httpkit]: http://http-kit.org/
[chord]: https://github.com/james-henderson/chord
[async]: https://github.com/clojure/core.async
[react]: http://facebook.github.io/react/
[om]: https://github.com/swannodette/om
[reagent]: https://reagent-project.github.io/
[hiccup]: https://github.com/weavejester/hiccup

## Prerequisites

You will need [Leiningen][leiningen] installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, which will be available on
port 8080 by default, run:

    lein run

Additionally, to enable live reloading of ClojureScript and CSS
via [Figwheel][figwheel], run:

    lein figwheel

[figwheel]: https://github.com/bhauman/lein-figwheel

The game uses a secure WebSocket (`wss://`) which requires some configuration to
work properly on localhost. The `local-dev` branch changes the WebSocket to be
insecure (`ws://`) so it will work out-of-the-box for local development.

## TODO

- Bidder goes out
- Teams
- Switch players/teams after game

## License

Released under the MIT license.

Copyright © 2014 Timothy Licata
