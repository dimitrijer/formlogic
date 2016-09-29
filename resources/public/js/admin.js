var myApp = angular.module('myApp', ['ui.bootstrap', 'ngSanitize']);

myApp.controller('TypeaheadCtrl', function($scope, $http) {
	$scope.studentProgresses = null;
	$scope.assignmentProgresses = null;

	$scope.getStudents = function(val) {
		return $http.get('/user/students', {
			params : {
				email: val
			}
		}).then(function(resp) {
			return resp.data.results;
		});
	};
	$scope.onStudentSelected = function(student) {
		$http.get('/user/students/progresses', {
			params : {
				id : student.id
			}
		}).then(function(resp) {
			$scope.studentProgresses = resp.data.results;
		});
	};
	$scope.getTests = function(val) {
		return $http.get('/user/assignments', {
			params : {
				assignment_name: val
			}
		}).then(function(resp) {
			return resp.data.results;
		});
	};
	$scope.onTestSelected = function(assignment) {
		$http.get('/user/assignments/progresses', {
			params : {
				id : assignment.id
			}
		}).then(function(resp) {
			$scope.assignmentProgresses = resp.data.results;
		});
	};
});
