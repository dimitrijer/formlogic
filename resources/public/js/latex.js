var myApp = angular.module('myApp', ['ui.bootstrap', 'ngSanitize']);

// Form controller.
myApp.controller('LatexFormController', ['$scope', '$http', function($scope, $http) {
	$scope.formula = "";
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
			console.log(response.data.result);
			$scope.result = response.data.result;
			$scope.error = null;
			katex.render(response.data.step0, document.getElementById("katexEquation0"));
			katex.render(response.data.step1, document.getElementById("katexEquation1"));
			katex.render(response.data.step2, document.getElementById("katexEquation2"));
			katex.render(response.data.step3, document.getElementById("katexEquation3"));
			katex.render(response.data.step4, document.getElementById("katexEquation4"));
			katex.render(response.data.step5, document.getElementById("katexEquation5"));
			katex.render(response.data.step6, document.getElementById("katexEquation6"));
			katex.render(response.data.step7[0], document.getElementById("katexEquation7"));
		}, function failedCallback(response, status) {
			console.log("Greska!");
			$scope.error = response.data.error;
		});
	};
}]);
