from instruments.InstDriver import InstrumentDriver


class VNA(InstrumentDriver):
    def __init__(self, config_json):
        super().__init__(config_json)
        self.type = "VNA"


    def write_test(self):
        if not self.check_connection():
            return
