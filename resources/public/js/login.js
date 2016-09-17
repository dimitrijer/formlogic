var myApp = angular.module('myApp', ['ui.bootstrap', 'ngSanitize']);
myApp.controller('LoginFormController', ['$scope', '$http', '$window',
	function($scope, $http, $window) {
		$scope.user = {};
		$scope.alert = null;
		$scope.antiForgeryToken = angular.element(document.querySelector('#__anti-forgery-token')).val();
		$scope.login = function() {
			userData = {
				email : $scope.user.email,
				password : md5($scope.user.password)
			};
			$http({
				method: 'POST',
				url: '/login',
				data: userData,
				headers: {
					"X-CSRF-Token" : $scope.antiForgeryToken
				}
			}).then(function successCallback(response) {
				console.log(response);
				$window.location.href = "/user/" + response["data"]["user-id"] + "/";
			}, function failedCallback(response, status) {
				console.log(response);
				$scope.alert = response["data"]["alert"];
			});
		}
}]);
