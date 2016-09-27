var myApp = angular.module('myApp', ['ui.bootstrap', 'ngSanitize']);

// Form controller.
myApp.controller('TaskFormController', ['$scope', '$uibModal', function($scope, $uibModal) {
	$scope.form = document.querySelector('#taskForm');
       	$scope.base_action = angular.element($scope.form).attr('data-action-uri');
	$scope.onSubmitNext = function() {
		$scope.form.action = $scope.base_action.concat('?continue=true');
		$scope.form.submit();
	};
	$scope.onSubmitPrev = function() {
		$scope.form.action = $scope.base_action.concat('?continue=false');
		$scope.form.submit();
	};
	$scope.openOkModal = function() {
		var modalInstance = $uibModal.open({
			animation: false,
			ariaLabelledBy: 'modal-title',
			ariaDescribedBy: 'modal-body',
			templateUrl: 'ok-modal.html',
			controller: 'ModalInstanceController',
			controllerAs: '$ctrl',
			size: 'md'
		});

		modalInstance.result.then(function (result) {
			$scope.onSubmitNext();
		});
	};
}]);

myApp.controller('ModalInstanceController', ['$uibModalInstance', function($uibModalInstance) {
	var $ctrl = this;
	$ctrl.ok = function() {
		$uibModalInstance.close(true);
	};
	$ctrl.cancel = function() {
		$uibModalInstance.dismiss('cancel');
	};
}]);
