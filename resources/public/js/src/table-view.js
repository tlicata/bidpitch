goog.provide("socky.table");

socky.table = (function () {
  return {
    componentDidMount: function (om) {
      var dom = om.owner.getDOMNode();
      console.log("componentDidMount", dom);
    },
    render: function (data) {
      console.log("render", data);
      return React.createElement("div", {style: {color: "black"}}, []);
    }
  };
})();
