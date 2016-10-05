var myApp = angular.module('myApp', ['ui.bootstrap', 'ngSanitize']);

// Form controller.
myApp.controller('LatexFormController', ['$scope', '$http', function($scope, $http) {
	$scope.formula = "";
	// Find AF token on the page.
	$scope.antiForgeryToken = angular.element(document.querySelector('#__anti-forgery-token')).val();
	$scope.result = {}
	$scope.error = null;
	$scope.sendFormula = function() {
		formulaData = {
			formula : $scope.formula
		};
		$http({
			method: 'POST',
			url: '/latex',
			data: formulaData,
			headers: {
				"X-CSRF-Token" : $scope.antiForgeryToken
			}
		}).then(function successCallback(response) {
			$scope.result = response.data.result;
			$scope.error = null;
			katex.render(response.data.result, document.getElementById("katexEquation"));
		}, function failedCallback(response, status) {
			$scope.error = response.data.error;
		});
	};
}]);
