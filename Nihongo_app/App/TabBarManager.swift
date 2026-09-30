import SwiftUI
import Observation

@Observable
final class TabBarManager {
    static let shared = TabBarManager()
    var isHidden = false
}
