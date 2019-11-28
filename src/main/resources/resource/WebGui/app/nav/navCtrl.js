angular.module('mrlapp.nav').controller('navCtrl', ['$scope', '$log', '$filter', '$timeout', '$location', '$anchorScroll', '$state', '$uibModal', 'mrl', 'statusSvc', 'panelSvc', 'noWorkySvc', 'Flash', function($scope, $log, $filter, $timeout, $location, $anchorScroll, $state, $uibModal, mrl, statusSvc, panelSvc, noWorkySvc, Flash) {
    //connection state LED
    $scope.connected = mrl.isConnected()

    $scope.errorStatus = null
    $scope.warningStatus = null
    $scope.infoStatus = null

    $scope.errorCount = 0
    $scope.warningCount = 0
    $scope.infoCount = 0

    mrl.subscribeConnected(function(connected) {
        $log.info('nav:connection update', connected)
        $timeout(function() {
            $scope.connected = connected
        })
    })

    // load type ahead service types
    $scope.possibleServices = Object.values(mrl.getPossibleServices())
    // get platform information for display
    $scope.platform = mrl.getPlatform()
    // status info warn error
    $scope.statusList = statusSvc.getStatuses()
    statusSvc.subscribeToUpdates(function(status) {
        $timeout(function() {
            if (status.level == "error") {
                $scope.errorStatus = status
                $scope.errorCount += 1
            } else if (status.level == "warn") {
                $scope.warningStatus = status
                $scope.warningCount += 1
            } else {
                $scope.infoStatus = status;
                $scope.infoCount += 1
            }
        })
    })

    $scope.showAll = panelSvc.showAll
    $scope.remoteId = mrl.getRemoteId();
    $scope.id = mrl.getId();
    $scope.platform.vmVersion
    if ($scope.platform.vmVersion != '1.8') {
        $scope.status = {
            level: "error",
            key: "BadJVM",
            detail: "unsupported Java " + $scope.platform.vmVersion + "- please uninstall and install Java 1.8"
        }
    }

    //service-panels & update-routine (also used for search)
    // populated for search

    var panelsUpdated = function(panels) {
        $scope.panels = panels
        // $scope.minlist = $filter('panellist')($scope.panels, 'min')
    }

    // maintains some for of subscription ... onRegistered I'd assume
    panelSvc.subscribeToUpdates(panelsUpdated)

    $scope.shutdown = function(type) {
        var modalInstance = $uibModal.open({
            animation: true,
            templateUrl: 'nav/shutdown.html',
            controller: 'shutdownCtrl',
            resolve: {
                type: function() {
                    return type
                }
            }
        })
    }

    $scope.about = function() {
        var modalInstance = $uibModal.open({
            animation: true,
            templateUrl: 'nav/about.html',
            controller: 'aboutCtrl'
        })
    }

    $scope.help = function() {
        // should be something with help - for now: no Worky
        //-> maybe tipps & tricks, ...
        noWorkySvc.openNoWorkyModal('')
    }

    $scope.noWorky = function() {
        // modal display of no worky 
        noWorkySvc.openNoWorkyModal('')
    }

    //START_Search
    //panels are retrieved above (together with minlist)
    $log.info('searchPanels', $scope.panels)
    $scope.searchOnSelect = function(item, model, label) {
        //expand panel if minified
        if (item.list == 'min') {
            item.panelsize.aktsize = item.panelsize.oldsize
            panelSvc.movePanelToList(item.name)
        }
        //show panel if hidden
        if (item.hide) {
            item.hide = false
        }
        //put panel on top
        panelSvc.putPanelZIndexOnTop(item.name)
        item.notifyZIndexChanged()
        //move panel to top of page
        item.posX = 15
        item.posY = 0
        item.notifyPositionChanged()
        $scope.searchSelectedPanel = ''
    }

    //END_Search
    //quick-start a service
    $scope.start = function(newName, newTypeModel) {
        mrl.sendTo(mrl.getRuntime().name, "start", newName, newTypeModel.name)
        $scope.newName = ''
        $scope.newType = ''
    }

    $scope.stateGo = $state.go
}
])
