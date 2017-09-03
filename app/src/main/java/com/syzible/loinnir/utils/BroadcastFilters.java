package com.syzible.loinnir.utils;

/**
 * Created by ed on 10/07/2017.
 */

public enum BroadcastFilters {
    finish_main_activity {
        @Override
        public String toString() {
            return "com.syzible.loinnir.finish_main_activity";
        }
    },

    new_locality_info_update {
        @Override
        public String toString() {
            return "com.syzible.loinnir.new_locality_update";
        }
    },

    changed_locality {
        @Override
        public String toString() {
            return "com.syzible.loinnir.changed_locality";
        }
    },

    updated_location {
        @Override
        public String toString() {
            return "com.syzible.loinnir.updated_location";
        }
    },

    new_partner_message {
        @Override
        public String toString() {
            return "com.syzible.loinnir.new_partner_message";
        }
    },

    block_enacted {
        @Override
        public String toString() {
            return "com.syzible.loinnir.block_enacted";
        }
    }
}
