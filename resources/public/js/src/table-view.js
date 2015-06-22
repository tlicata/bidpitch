goog.provide("socky.table");

socky.table = (function () {
  return {
    componentDidMount: function (om) {
      var canvas = om.owner.getDOMNode();
      console.log("componentDidMount", canvas);
    },
    render: function (data) {
      console.log("render", data);
      var tableCards = data["table-cards"];
      var players = data["players"];
      var playerElements = [];
      if (tableCards && players && players.map) {
        playerElements = players.map(function (player, idx) {
          var name = data.me === players[idx] ? "You" : players[idx];
          var text = React.createElement("span", {}, name);
          var cardOpts = {className: "card"};
          if (tableCards.length > idx) {
            cardOpts.src = "/img/cards/individual/" + tableCards[idx] + ".svg?2.09"
          }
          var card = React.createElement("img", cardOpts, []);
          return React.createElement("div", {
            style: {
              alignItems: "center",
              display: "flex",
              flexDirection: "column"
            }
          }, [text, card]);
        });
      }
      return React.createElement("div", {
        style: {
          alignItems: "flex-start",
          display: "flex",
          flexDirection: "row"
        }
      }, playerElements);
    }
  };
})();
