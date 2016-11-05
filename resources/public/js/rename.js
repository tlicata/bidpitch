document.cookie = "username=fake;path=/;expires=Thu, 01 Jan 1970 00:00:01 GMT";
if (window.localStorage) {
  window.localStorage.removeItem("username");
}
